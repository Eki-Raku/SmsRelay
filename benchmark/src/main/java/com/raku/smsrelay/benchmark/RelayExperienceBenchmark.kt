package com.raku.smsrelay.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RelayExperienceBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun navigationConversationSheetAndHaze() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        setupBlock = {
            pressHome()
            device.executeShellCommand("cmd role add-role-holder android.app.role.SMS $PACKAGE_NAME")
            listOf(
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.SEND_SMS",
            ).forEach { permission ->
                device.executeShellCommand("pm grant $PACKAGE_NAME $permission")
            }
            device.executeShellCommand(
                "content insert --uri content://sms/inbox " +
                    "--bind address:s:10086 --bind body:s:体验基准短信 --bind date:l:1777500000000",
            )
            startActivityAndWait()
            dismissOnboarding(required = false)
        },
    ) {
        repeat(2) {
            clickRequiredText("短信")
            clickRequiredText("10086")
            device.pressBack()
            device.waitForIdle()
            clickRequiredDescription("新建短信")
            check(device.wait(Until.hasObject(By.text("新信息")), UI_TIMEOUT_MS)) {
                "Compose message sheet did not appear"
            }
            device.pressBack()
            clickRequiredText("转发")
            clickRequiredText("设置")
            clickRequiredText("重新查看初始化引导")
            dismissOnboarding(required = true)
            check(device.wait(Until.hasObject(By.text("短信信使")), UI_TIMEOUT_LONG_MS)) {
                "Status page did not settle after dismissing onboarding"
            }
            clickRequiredText("设置")
            check(device.wait(Until.hasObject(By.text("重新查看初始化引导")), UI_TIMEOUT_LONG_MS)) {
                "Settings page did not appear"
            }
            scrollUntilTextVisible("QQ SMTP")
            clickRequiredText("QQ SMTP")
            device.pressBack()
            device.waitForIdle()
            clickRequiredText("状态")
        }
        device.pressHome()
        device.waitForIdle()
        killProcess()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.dismissOnboarding(required: Boolean) {
        val selector = By.text("跳过")
        val firstMatch = device.wait(
            Until.findObject(selector),
            if (required) UI_TIMEOUT_LONG_MS else UI_TIMEOUT_MS,
        )
        if (firstMatch == null) {
            check(!required) { "Onboarding skip action did not appear" }
            return
        }

        repeat(3) {
            val skip = device.wait(Until.findObject(selector), UI_TIMEOUT_MS)
                ?: return
            if (runCatching { skip.click() }.isSuccess &&
                device.wait(Until.gone(selector), UI_TIMEOUT_MS)
            ) {
                device.waitForIdle()
                return
            }
        }
        error("Onboarding did not dismiss")
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.clickRequiredText(text: String) {
        repeat(3) {
            val node = device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS) ?: return@repeat
            if (runCatching { node.click() }.isSuccess) {
                device.waitForIdle()
                return
            }
        }
        error("Required text could not be clicked: $text")
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.clickRequiredDescription(description: String) {
        repeat(3) {
            val node = device.wait(Until.findObject(By.desc(description)), UI_TIMEOUT_MS) ?: return@repeat
            if (runCatching { node.click() }.isSuccess) {
                device.waitForIdle()
                return
            }
        }
        error("Required control could not be clicked: $description")
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.scrollUntilTextVisible(text: String) {
        repeat(4) {
            if (device.hasObject(By.text(text))) return
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 3,
                12,
            )
            device.waitForIdle()
        }
        check(device.hasObject(By.text(text))) { "Required text could not be reached by scrolling: $text" }
    }

    private companion object {
        const val PACKAGE_NAME = "com.raku.smsrelay"
        const val UI_TIMEOUT_MS = 3_000L
        const val UI_TIMEOUT_LONG_MS = 8_000L
    }
}
