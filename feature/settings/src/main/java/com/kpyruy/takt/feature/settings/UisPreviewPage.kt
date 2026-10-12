package com.kpyruy.takt.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.UniversityAccountRepository
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.TaktIconButton
import com.kpyruy.takt.core.ui.i18n.t

@Composable
fun UisPreviewPage(repository: UniversityAccountRepository, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader(t("Зміни з UIS"), subtitle = t("Переглянь перед застосуванням"), navigation = {
            TaktIconButton(Icons.AutoMirrored.Outlined.ArrowBack, t("Назад"), onBack)
        })
        UisSyncReview(repository, showHeading = false)
    }
}
