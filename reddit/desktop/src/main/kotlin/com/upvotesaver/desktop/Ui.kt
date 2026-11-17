package com.upvotesaver.desktop

import com.upvotesaver.core.PostRecord
import com.upvotesaver.core.Redgifs
import com.upvotesaver.core.RedgifsGif
import com.upvotesaver.core.RedgifsNiche
import com.upvotesaver.core.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Desktop
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Image
import java.awt.Insets
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import javax.imageio.ImageIO
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

private fun ui(block: () -> Unit) = SwingUtilities.invokeLater(block)

/** The app icon: an up arrow above a tray, drawn here so no image file is needed. */
private fun appIcon(size: Int): Image {
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color(0xC2, 0x41, 0x0C)
    g.fillRoundRect(0, 0, size, size, size / 4, size / 4)
    g.color = Color.WHITE
    fun p(v: Double) = (v * size).toInt()
    g.fillPolygon(intArrayOf(p(.5), p(.76), p(.6), p(.6), p(.4), p(.4), p(.24)), intArrayOf(p(.14), p(.42), p(.42), p(.62), p(.62), p(.42), p(.42)), 7)
    g.fillRect(p(.2), p(.72), p(.6), p(.12))
    g.dispose()
    return image
}

class MainWindow(private val app: App, startInTray: Boolean) {
    private val frame = JFrame("Upvote Saver")
    private val status = JLabel(" ")
    private val progress = JProgressBar().apply { isIndeterminate = true; isVisible = false; preferredSize = Dimension(120, 14) }
    private val cancel = JButton(t("Cancel", "İptal")).apply { isVisible = false; addActionListener { app.cancel() } }
    private val download = DownloadTab(app)
    private val niches = NicheTab(app)
    private var tray: TrayIcon? = null

    init {
        frame.iconImages = listOf(appIcon(16), appIcon(32), appIcon(64))
        val tabs = JTabbedPane()
        tabs.addTab(t("Download", "İndir"), download.panel)
        tabs.addTab(t("Niches", "Niche'ler"), niches.panel)
        tabs.addTab(t("Settings", "Ayarlar"), SettingsTab(app).panel)
        val bar = JPanel(BorderLayout(8, 0)).apply {
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            add(status, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply { add(progress); add(cancel) }, BorderLayout.EAST)
        }
        frame.contentPane.add(tabs, BorderLayout.CENTER)
        frame.contentPane.add(bar, BorderLayout.SOUTH)
        frame.setSize(940, 660)
        frame.setLocationRelativeTo(null)

        app.onStatus = { text, busy -> ui { status.text = text.ifEmpty { " " }; progress.isVisible = busy; cancel.isVisible = busy } }
        app.onChanged = { ui { download.refresh(); niches.table.repaint() } }

        setUpTray()
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            // With automatic checks on, closing the window keeps the app in the tray.
            override fun windowClosing(e: WindowEvent) {
                if (tray != null && app.settings.intervalMinutes > 0 && app.settings.user.isNotEmpty()) {
                    frame.isVisible = false
                    tray?.displayMessage("Upvote Saver", t("Still running here to check your likes.", "Beğenilerini denetlemek için burada çalışmaya devam ediyor."), TrayIcon.MessageType.INFO)
                } else {
                    System.exit(0)
                }
            }
        })
        frame.isVisible = !(startInTray && tray != null)
    }

    private fun setUpTray() {
        if (!SystemTray.isSupported()) return
        val menu = PopupMenu()
        menu.add(MenuItem(t("Open", "Aç")).apply { addActionListener { show() } })
        menu.add(MenuItem(t("Check for new likes", "Yeni beğenileri denetle")).apply { addActionListener { app.syncLikes(false) } })
        menu.add(MenuItem(t("Exit", "Çıkış")).apply { addActionListener { System.exit(0) } })
        val icon = TrayIcon(appIcon(32), "Upvote Saver", menu).apply { isImageAutoSize = true; addActionListener { show() } }
        tray = runCatching { SystemTray.getSystemTray().add(icon); icon }.getOrNull()
    }

    private fun show() = ui { frame.isVisible = true; frame.toFront() }
}

private fun row(vararg parts: java.awt.Component) = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4)).apply { parts.forEach { add(it) } }

/** Account, likes, a link field and the list of what was saved. */
private class DownloadTab(private val app: App) {
    private val account = JLabel()
    private val signIn = JButton(t("Sign in to Redgifs", "Redgifs'e giriş yap")).apply { addActionListener { app.signIn() } }
    private val signOut = JButton(t("Sign out", "Çıkış yap")).apply { addActionListener { app.signOut() } }
    private val newLikes = JButton(t("Check for new likes", "Yeni beğenileri denetle")).apply { addActionListener { app.syncLikes(false) } }
    private val allLikes = JButton(t("Download all my likes", "Tüm beğenilerimi indir")).apply { addActionListener { app.syncLikes(true) } }
    private val link = JTextField(40)
    private val counts = JLabel()
    private val model = RecentModel()
    private val table = JTable(model)
    val panel = JPanel(BorderLayout())

    init {
        val send = JButton(t("Download", "İndir"))
        val go = {
            if (link.text.isNotBlank()) {
                if (app.saveLinks(link.text)) link.text = ""
                else app.onStatus(t("No Redgifs link found in that text.", "Bu metinde Redgifs bağlantısı bulunamadı."), false)
            }
        }
        send.addActionListener { go() }
        link.addActionListener { go() }
        val top = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(8, 8, 4, 8)
            add(row(account, signIn, signOut))
            add(row(newLikes, allLikes))
            add(row(JLabel(t("Redgifs link:", "Redgifs bağlantısı:")), link, send))
            add(row(counts))
        }
        table.rowHeight = 22
        table.autoCreateRowSorter = false
        table.columnModel.getColumn(0).preferredWidth = 220
        table.columnModel.getColumn(2).preferredWidth = 320
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) model.rows.getOrNull(table.selectedRow)?.let { r ->
                    runCatching { Desktop.getDesktop().browse(URI(r.permalink)) }
                }
            }
        })
        panel.add(top, BorderLayout.NORTH)
        panel.add(JScrollPane(table), BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        val s = app.settings
        account.text = when {
            app.signedIn -> t("Signed in as ${s.user}", "Giriş yapıldı: ${s.user}")
            s.user.isNotEmpty() -> t("Sign-in expired (${s.user})", "Oturumun süresi doldu (${s.user})")
            else -> t("Not signed in", "Giriş yapılmadı")
        }
        signOut.isVisible = s.user.isNotEmpty()
        signIn.isVisible = !app.signedIn
        newLikes.isEnabled = s.user.isNotEmpty()
        allLikes.isEnabled = s.user.isNotEmpty()
        val c = app.store.counts()
        counts.text = t("${c[Status.DONE] ?: 0} saved · ${c[Status.FAILED] ?: 0} failed · folder: ${s.folder}",
            "${c[Status.DONE] ?: 0} kaydedildi · ${c[Status.FAILED] ?: 0} başarısız · klasör: ${s.folder}")
        model.rows = app.store.recent(500)
        model.fireTableDataChanged()
    }

    private class RecentModel : AbstractTableModel() {
        var rows: List<PostRecord> = emptyList()
        private val names = listOf(t("Posted by", "Yükleyen"), t("Status", "Durum"), t("Note", "Not"), t("Time", "Zaman"))
        private val time = SimpleDateFormat("yyyy-MM-dd HH:mm")
        override fun getRowCount() = rows.size
        override fun getColumnCount() = names.size
        override fun getColumnName(column: Int) = names[column]
        override fun getValueAt(row: Int, column: Int): Any = rows[row].let { r ->
            when (column) {
                0 -> r.title
                1 -> if (r.status == Status.DONE) t("Saved", "Kaydedildi") else if (r.status == Status.FAILED) t("Failed", "Başarısız") else t("Nothing to save", "Kaydedilecek bir şey yok")
                2 -> r.permalink.substringAfterLast('/') + if (r.status == Status.FAILED) " · ${r.note}" else ""
                else -> time.format(Date(r.timeMs))
            }
        }
    }
}

/** Search niches, list one niche's clips in a chosen order, tick and download. */
private class NicheTab(private val app: App) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val query = JTextField(18)
    private val listModel = DefaultListModel<RedgifsNiche>()
    private val list = JList(listModel)
    private val orders = Redgifs.NICHE_ORDERS
    private val orderLabels = listOf(t("Most liked", "En beğenilen"), t("Hot", "Popüler"), t("Newest", "En yeni"))
    private val order = JComboBox(orderLabels.toTypedArray())
    private val downloadButton = JButton()
    private val more = JButton(t("Load more", "Daha fazla yükle")).apply { isEnabled = false }
    private val info = JLabel(" ")
    private val model = GifModel()
    val table = JTable(model)
    val panel = JPanel(BorderLayout())
    private var niche: RedgifsNiche? = null
    private var page = 0

    init {
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = javax.swing.DefaultListCellRenderer().let { base ->
            javax.swing.ListCellRenderer<RedgifsNiche> { l, value, index, selected, focus ->
                base.getListCellRendererComponent(l, "${value.name}  (${value.gifs})", index, selected, focus)
            }
        }
        list.addListSelectionListener { if (!it.valueIsAdjusting) list.selectedValue?.let(::open) }
        val search = JButton(t("Search", "Ara"))
        search.addActionListener { searchNiches() }
        query.addActionListener { searchNiches() }
        val left = JPanel(BorderLayout()).apply {
            add(JPanel(BorderLayout(6, 0)).apply {
                border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
                add(query, BorderLayout.CENTER)
                add(search, BorderLayout.EAST)
            }, BorderLayout.NORTH)
            add(JScrollPane(list), BorderLayout.CENTER)
        }
        val selectAll = JButton(t("Select all", "Tümünü seç"))
        selectAll.addActionListener { model.selectAll(); updateButton() }
        order.addActionListener { niche?.let(::open) }
        downloadButton.addActionListener {
            val ids = model.selectedIds()
            if (ids.isNotEmpty()) { app.saveIds(ids); model.clearSelection(); updateButton() }
        }
        more.addActionListener { load() }
        table.rowHeight = 60
        table.columnModel.getColumn(0).maxWidth = 40
        table.columnModel.getColumn(1).maxWidth = 110
        table.columnModel.getColumn(1).preferredWidth = 110
        model.onChange = { updateButton() }
        val right = JPanel(BorderLayout()).apply {
            add(row(order, selectAll, downloadButton, more, info), BorderLayout.NORTH)
            add(JScrollPane(table), BorderLayout.CENTER)
        }
        panel.add(JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right).apply { dividerLocation = 260 }, BorderLayout.CENTER)
        updateButton()
        searchNiches()
    }

    private fun updateButton() {
        val n = model.selectedIds().size
        downloadButton.text = t("Download ($n)", "İndir ($n)")
        downloadButton.isEnabled = n > 0
    }

    private fun call(block: suspend () -> Unit) {
        info.text = t("Loading…", "Yükleniyor…")
        scope.launch {
            val error = runCatching { block() }.exceptionOrNull()
            ui { info.text = if (error != null) t("Error: ", "Hata: ") + (error.message ?: error.javaClass.simpleName) else " " }
        }
    }

    private fun searchNiches() {
        val text = query.text
        call {
            val found = app.redgifs.niches(text)
            ui { listModel.clear(); found.forEach(listModel::addElement) }
        }
    }

    private fun open(picked: RedgifsNiche) {
        niche = picked
        page = 0
        model.clear()
        more.isEnabled = false
        updateButton()
        load()
    }

    private fun load() {
        val picked = niche ?: return
        val sort = orders[order.selectedIndex.coerceAtLeast(0)]
        val next = page + 1
        call {
            val result = app.redgifs.nicheGifs(picked.id, sort, next)
            ui {
                if (niche?.id != picked.id || orders[order.selectedIndex.coerceAtLeast(0)] != sort) return@ui
                page = next
                model.add(result.gifs)
                more.isEnabled = result.hasMore
            }
            // Thumbnails arrive one by one after the list is shown.
            for (gif in result.gifs) {
                if (gif.thumbnail.isEmpty()) continue
                val icon = runCatching {
                    val bytes = app.http.call(gif.thumbnail) { it.body?.bytes() ?: ByteArray(0) }
                    ImageIO.read(bytes.inputStream())?.let { ImageIcon(it.getScaledInstance(-1, 56, Image.SCALE_SMOOTH)) }
                }.getOrNull() ?: continue
                ui { model.setIcon(gif.id, icon) }
            }
        }
    }

    private inner class GifModel : AbstractTableModel() {
        private val rows = mutableListOf<RedgifsGif>()
        private val selected = HashSet<String>()
        private val icons = HashMap<String, ImageIcon>()
        var onChange: () -> Unit = {}
        private val names = listOf("", "", t("Posted by", "Yükleyen"), t("Likes", "Beğeni"), t("Length", "Süre"), t("Sound", "Ses"), t("Status", "Durum"))

        fun clear() { rows.clear(); selected.clear(); fireTableDataChanged() }
        fun add(gifs: List<RedgifsGif>) { val known = rows.map { it.id }.toSet(); rows += gifs.filter { it.id !in known }; fireTableDataChanged() }
        fun setIcon(id: String, icon: ImageIcon) { icons[id] = icon; rows.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { fireTableCellUpdated(it, 1) } }
        fun selectedIds() = rows.map { it.id }.filter { it in selected }
        fun clearSelection() { selected.clear(); fireTableDataChanged() }
        fun selectAll() { if (selected.size == rows.size) selected.clear() else selected += rows.map { it.id }; fireTableDataChanged() }

        override fun getRowCount() = rows.size
        override fun getColumnCount() = names.size
        override fun getColumnName(column: Int) = names[column]
        override fun getColumnClass(column: Int): Class<*> = when (column) {
            0 -> java.lang.Boolean::class.java
            1 -> ImageIcon::class.java
            else -> String::class.java
        }
        override fun isCellEditable(row: Int, column: Int) = column == 0
        override fun setValueAt(value: Any?, row: Int, column: Int) {
            val id = rows[row].id
            if (value == true) selected += id else selected -= id
            onChange()
        }
        override fun getValueAt(row: Int, column: Int): Any? = rows[row].let { g ->
            when (column) {
                0 -> g.id in selected
                1 -> icons[g.id]
                2 -> g.user.ifEmpty { g.id }
                3 -> g.likes.toString()
                4 -> "${g.durationSeconds.toInt()} s"
                5 -> if (g.hasAudio) t("yes", "var") else ""
                else -> if (app.store.get("rg_${g.id}")?.status == Status.DONE) t("Already saved", "Daha önce indirildi") else ""
            }
        }
    }
}

/** Download folder, hiding it, the schedule, starting with Windows, and the browser used for sign-in. */
private class SettingsTab(private val app: App) {
    val panel = JPanel(GridBagLayout())

    init {
        val s = app.settings
        val folder = JTextField(s.folder.path, 36).apply { isEditable = false }
        val choose = JButton(t("Choose…", "Seç…"))
        choose.addActionListener {
            val chooser = JFileChooser(s.folder).apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
            if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                s.folder = chooser.selectedFile
                folder.text = s.folder.path
                FolderSink.prepareFolder(s)
                app.onChanged()
            }
        }
        val openFolder = JButton(t("Open folder", "Klasörü aç"))
        openFolder.addActionListener { runCatching { Desktop.getDesktop().open(FolderSink.prepareFolder(s)) } }

        val hidden = JCheckBox(t("Hide the download folder (Windows \"hidden\" attribute)", "İndirme klasörünü gizle (Windows \"gizli\" özniteliği)"), s.hidden)
        hidden.addActionListener { s.hidden = hidden.isSelected; FolderSink.prepareFolder(s) }

        val minutes = listOf(0, 15, 60, 360, 1440)
        val labels = listOf(t("Off", "Kapalı"), t("Every 15 minutes", "15 dakikada bir"), t("Every hour", "Saatte bir"), t("Every 6 hours", "6 saatte bir"), t("Daily", "Günde bir"))
        val interval = JComboBox(labels.toTypedArray())
        interval.selectedIndex = minutes.indexOf(s.intervalMinutes).coerceAtLeast(0)
        interval.addActionListener { s.intervalMinutes = minutes[interval.selectedIndex] }

        val autostart = JCheckBox(t("Start with Windows (in the system tray)", "Windows ile başlat (sistem tepsisinde)"), Autostart.enabled)
        autostart.isEnabled = Autostart.supported
        autostart.addActionListener { runCatching { Autostart.set(autostart.isSelected) }; autostart.isSelected = Autostart.enabled }

        val browser = JTextField(s.browserPath, 36)
        browser.addActionListener { s.browserPath = browser.text }
        browser.addFocusListener(object : java.awt.event.FocusAdapter() {
            override fun focusLost(e: java.awt.event.FocusEvent) { s.browserPath = browser.text }
        })

        var y = 0
        fun line(label: String?, vararg parts: java.awt.Component) {
            val c = GridBagConstraints().apply { gridy = y; insets = Insets(6, 10, 6, 6); anchor = GridBagConstraints.WEST }
            if (label != null) panel.add(JLabel(label), c.apply { gridx = 0 })
            panel.add(row(*parts), c.apply { gridx = 1; weightx = 1.0; fill = GridBagConstraints.HORIZONTAL })
            y++
        }
        line(t("Download folder", "İndirme klasörü"), folder, choose, openFolder)
        line(null, hidden)
        line(t("Check my likes", "Beğenilerimi denetle"), interval,
            JLabel(t("(while the app is running)", "(uygulama çalışırken)")))
        line(null, autostart)
        line(t("Browser for sign-in", "Giriş için tarayıcı"), browser,
            JLabel(t("(blank = Edge, Chrome or Brave)", "(boş = Edge, Chrome ya da Brave)")))
        panel.add(Box.createVerticalGlue(), GridBagConstraints().apply { gridy = y; weighty = 1.0 })
    }
}
