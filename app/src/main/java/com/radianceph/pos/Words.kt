package com.radianceph.pos

/**
 * The app's words in English and Taglish (the owner's terms: Kahera, Imbentaryo; never "cashier", "inventory",
 * "staff", "puwede", "kaha" in the Taglish), chosen by the shop's language as POS V3 has it (tl-PH: Taglish).
 * The shop's own answers (refusals) come already in the person's language from the shop.
 */
object Words {
    @Volatile var tl: Boolean = false

    fun use(language: String) {
        tl = language.startsWith("tl")
    }

    private val en = mapOf(
        "link_title" to "Link this phone",
        "link_lead" to "Ask the owner to open Office > Phones on the POS and tap Link a phone beside your name. Then scan the QR on that screen.",
        "link_scan" to "Scan the owner's QR",
        "link_working" to "Linking…",
        "linked_proven" to "Linked. This phone proved it's yours and untouched: it can clock you in and out.",
        "linked_scan_only" to "Linked for scanning. This phone couldn't prove itself (%s), so it can't clock you in.",
        "not_a_link" to "That isn't a link QR from a Radiance POS shop.",
        "hi" to "Hi, %s",
        "scan_to_pc" to "Scan for my PC",
        "scan_to_pc_help" to "Each barcode goes into the cart on the PC where you're signed in.",
        "price_check" to "Price check",
        "price_check_help" to "See an item's price and stock.",
        "count" to "Count stock",
        "count_help" to "Scan and count; the stock is set to your counts.",
        "settings" to "This phone",
        "register" to "Sale",
        "receiving" to "Receiving",
        "sent" to "Sent",
        "open_pc" to "Open the sale (or receiving) on your PC: the items appear there.",
        "camera_ask" to "Allow the camera to read barcodes.",
        "camera_denied" to "The camera isn't allowed. Allow it in Settings > Apps > Radiance POS > Permissions.",
        "aim" to "Point the camera at a barcode",
        "price" to "Price",
        "stock" to "Stock",
        "scan_another" to "Scan another",
        "branch" to "Branch",
        "counted" to "Counted",
        "send_count" to "Save the counts",
        "confirm_count" to "Set the stock of %1\$d items at %2\$s to these counts?",
        "count_saved" to "Saved. Stock set to your counts.",
        "before_after" to "%1\$s → %2\$s",
        "nothing_counted" to "Nothing counted yet. Scan an item.",
        "yes" to "Yes, save",
        "cancel" to "Cancel",
        "back" to "Back",
        "retry" to "Try again",
        "unlink_here" to "Forget the link on this phone",
        "unlink_here_help" to "The owner can also unlink it on the POS. To use this phone again, link it again.",
        "linked_to" to "Linked to %1\$s as %2\$s",
        "proven" to "Proven phone",
        "scan_only" to "Scan only",
        "offline" to "No connection to the shop. Check the internet and try again.",
        "loading" to "One moment…",
    )

    private val taglish = mapOf(
        "link_title" to "I-link ang phone na ito",
        "link_lead" to "Pabuksan sa may-ari ang Opisina > Mga phone sa POS at i-tap ang I-link ang phone katabi ng pangalan mo. Saka i-scan ang QR sa screen na iyon.",
        "link_scan" to "I-scan ang QR ng may-ari",
        "link_working" to "Nagli-link…",
        "linked_proven" to "Na-link. Napatunayan ng phone na ito na sa iyo ito at walang binago: makakapag-time in at out ka rito.",
        "linked_scan_only" to "Na-link para sa pag-scan. Hindi napatunayan ng phone na ito ang sarili (%s), kaya hindi ka makakapag-time in dito.",
        "not_a_link" to "Hindi ito QR para mag-link mula sa isang Radiance POS na shop.",
        "hi" to "Hi, %s",
        "scan_to_pc" to "Mag-scan para sa PC ko",
        "scan_to_pc_help" to "Papasok ang bawat barcode sa cart ng PC kung saan ka naka-sign in.",
        "price_check" to "Tingnan ang presyo",
        "price_check_help" to "Makita ang presyo at stock ng item.",
        "count" to "Bilangin ang stock",
        "count_help" to "Mag-scan at magbilang; ise-set ang stock ayon sa bilang mo.",
        "settings" to "Ang phone na ito",
        "register" to "Benta",
        "receiving" to "Receiving",
        "sent" to "Naipadala",
        "open_pc" to "Buksan ang benta (o receiving) sa PC mo: doon lalabas ang mga item.",
        "camera_ask" to "Payagan ang camera para mabasa ang mga barcode.",
        "camera_denied" to "Hindi pinapayagan ang camera. Payagan ito sa Settings > Apps > Radiance POS > Permissions.",
        "aim" to "Itapat ang camera sa barcode",
        "price" to "Presyo",
        "stock" to "Stock",
        "scan_another" to "Mag-scan ng iba",
        "branch" to "Branch",
        "counted" to "Nabilang",
        "send_count" to "I-save ang mga bilang",
        "confirm_count" to "I-set ang stock ng %1\$d na item sa %2\$s ayon sa mga bilang na ito?",
        "count_saved" to "Na-save. Naka-set ang stock ayon sa bilang mo.",
        "before_after" to "%1\$s → %2\$s",
        "nothing_counted" to "Wala pang nabilang. Mag-scan ng item.",
        "yes" to "Oo, i-save",
        "cancel" to "Huwag muna",
        "back" to "Bumalik",
        "retry" to "Subukan ulit",
        "unlink_here" to "Kalimutan ang link sa phone na ito",
        "unlink_here_help" to "Maaari rin itong i-unlink ng may-ari sa POS. Para magamit ulit ang phone, i-link ulit.",
        "linked_to" to "Naka-link sa %1\$s bilang %2\$s",
        "proven" to "Patunay na phone",
        "scan_only" to "Scan lang",
        "offline" to "Walang koneksyon sa shop. Tingnan ang internet at subukan ulit.",
        "loading" to "Sandali lang…",
    )

    fun t(key: String, vararg args: Any): String {
        val text = (if (tl) taglish[key] else en[key]) ?: en[key] ?: key
        return if (args.isEmpty()) text else String.format(text, *args)
    }
}
