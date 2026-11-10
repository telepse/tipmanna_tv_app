package com.example.telepse.di

import javax.inject.Qualifier

/** HttpClient without tipper [defaultRequest] auth — for animation-recovery claim/progress only. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RecoveryAuthorizedClient
