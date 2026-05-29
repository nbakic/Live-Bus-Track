package hr.zet.transit.ui.common

/**
 * Razlog neuspjelog učitavanja podataka — da korisnik zna je li problem do
 * njegove internetske veze ili do našeg poslužitelja.
 */
enum class LoadError { NO_INTERNET, SERVER }

/**
 * Klasificira neuspjeh: ako uređaj nije online → korisnikova veza; inače
 * (imamo mrežu, ali dohvat je pao) → naš poslužitelj.
 */
fun classifyLoadError(online: Boolean): LoadError =
    if (online) LoadError.SERVER else LoadError.NO_INTERNET
