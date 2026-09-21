package com.kpyruy.takt.feature.subjects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kpyruy.takt.core.data.StudyPlanRepository
import com.kpyruy.takt.core.model.CourseRequirementType
import com.kpyruy.takt.core.model.CourseStatus
import com.kpyruy.takt.core.ui.components.ScreenHeader
import com.kpyruy.takt.core.ui.components.SectionCard
import com.kpyruy.takt.core.ui.components.StatusPill

@Composable
fun SubjectDetailScreen(
    repository: StudyPlanRepository,
    courseId: String,
    onBack: () -> Unit,
) {
    val course by repository.observeCourse(courseId).collectAsState(initial = null)

    val item = course
    if (item == null) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            ScreenHeader(
                title = "Предмет",
                action = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
            Text("Предмет не знайдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            title = item.title,
            subtitle = item.code,
            action = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Семестр ${item.semester}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${item.credits} кредитів", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                StatusPill(
                    text = when (item.status) {
                        CourseStatus.FULFILLED -> "Закрито"
                        CourseStatus.ENROLLED -> "Активний"
                        CourseStatus.PLANNED -> "Заплановано"
                        CourseStatus.NOT_ENROLLED -> "Не записаний"
                        CourseStatus.NOT_NEEDED -> "Не потрібно"
                    }
                )
            }
        }

        SectionCard {
            Text("Оцінювання", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "0 / 100 балів",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Компоненти оцінювання ще не налаштовані. Тут будуть тести, лабораторні, домашки та екзамен.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Домашки та нотатки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Немає активних завдань або нотаток.",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard {
            Text("Про предмет", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = when (item.requirementType) {
                    CourseRequirementType.COMPULSORY -> "Обов'язковий предмет"
                    CourseRequirementType.SEMI_COMPULSORY -> "Обов'язково-вибірковий предмет"
                    CourseRequirementType.ELECTIVE -> "Вибірковий предмет"
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Стандартна шкала: A 92–100%, B 83–91%, C 74–82%, D 65–73%, E 56–64%, FX 0–55%.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
