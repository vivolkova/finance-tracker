class CurrencyNotFoundException(currency: String) :
    RuntimeException("Курс для валюты '$currency' не найден")

class ExternalRateException(message: String) : RuntimeException(message)