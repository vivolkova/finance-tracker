package com.example.financetracker.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Имя логгера = тип [T] на этапе компиляции (reified) → точное имя класса,
 * без риска поймать имя CGLIB-прокси (как у LoggerFactory.getLogger(this::class.java)).
 * Использование: private val logger = loggerFor<MyClass>()
 */
inline fun <reified T> loggerFor(): Logger = LoggerFactory.getLogger(T::class.java)
