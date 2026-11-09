package com.maysk.cloner.engine

/** Режим работы Google для конкретного профиля (приложения). */
enum class GoogleMode(val title: String, val subtitle: String) {
    STANDARD("Стандартный", "Работа Google Services по умолчанию"),
    NO_AUTO_GOOGLE("Без автоматического Google-запроса", "Не показывать запросы Google при запуске"),
    COMPAT("Совместимость", "Полная совместимость с Google Services"),
}

/**
 * Группы доменов Google/microG. Блокировка работает на уровне DNS: когда приложение
 * в режиме «Без автоматического Google-запроса» пытается определить адрес одного из этих
 * доменов, локальный файрвол отвечает «адрес не найден». Необязательный вход в Google
 * просто не доходит до сервера, а собственная авторизация приложения (другой домен) работает.
 *
 * Здесь НЕ блокируются домены, нужные самой игре (её API, игровой логин) — только
 * необязательные обращения к Google. Пароли не перехватываются, токены не подделываются.
 */
object BlockLists {
    /** Автопроверка «есть ли интернет/аккаунт» — именно она чаще всего и вызывает окно входа. */
    val CONNECTIVITY = listOf(
        "connectivitycheck.gstatic.com",
        "connectivitycheck.android.com",
        "clients3.google.com",
        "clients4.google.com",
        "www.google.com/generate_204",
    )

    /** Вход в аккаунт Google / Google Sign-In / AccountManager. */
    val SIGN_IN = listOf(
        "accounts.google.com",
        "accounts.google.ru",
        "accounts.youtube.com",
        "oauth2.googleapis.com",
        "www.googleapis.com/oauth2",
        "android.googleapis.com/auth",
        "android.clients.google.com",
    )

    /** Google Play Games Services. */
    val PLAY_GAMES = listOf(
        "games.googleapis.com",
        "games-pa.googleapis.com",
        "play.googleapis.com",
        "playgateway-pa.googleapis.com",
    )

    /** microG как поставщик Google API. */
    val MICROG = listOf(
        "microg.org",
        "mgoogle.com",
        "gms.microg",
    )

    /** Переход в магазин Google Play. */
    val PLAY_STORE = listOf(
        "play.google.com",
        "market.android.com",
        "android.clients.google.com/market",
    )

    /** Открытие внешнего браузера для авторизации Google. */
    val BROWSER_REDIRECT = listOf(
        "accounts.google.com/o/oauth2",
        "accounts.google.com/signin",
    )

    /** Домены, которые НИКОГДА не блокируются (FCM/push — иначе ломаются уведомления). */
    val NEVER_BLOCK = listOf(
        "mtalk.google.com",
        "fcm.googleapis.com",
        "firebaseinstallations.googleapis.com",
    )
}
