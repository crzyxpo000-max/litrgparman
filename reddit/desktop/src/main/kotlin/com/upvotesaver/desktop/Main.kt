package com.upvotesaver.desktop

import java.io.File
import java.io.RandomAccessFile
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main(args: Array<String>) {
    val dir = DesktopSettings.dataDir().also { it.mkdirs() }
    // One instance only: two would write the same download log.
    val lock = runCatching { RandomAccessFile(File(dir, "app.lock"), "rw").channel.tryLock() }.getOrNull()
    SwingUtilities.invokeLater {
        runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }
        if (lock == null) {
            JOptionPane.showMessageDialog(null, t("Upvote Saver is already running (look in the system tray).",
                "Upvote Saver zaten çalışıyor (sistem tepsisine bak)."))
            System.exit(0)
        }
        MainWindow(App(dir), startInTray = "--tray" in args)
    }
}
