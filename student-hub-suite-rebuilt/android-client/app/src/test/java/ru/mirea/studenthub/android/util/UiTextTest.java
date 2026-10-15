package ru.mirea.studenthub.android.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import ru.mirea.studenthub.android.model.ApiModels;

import static org.junit.Assert.assertEquals;

/** Проверяет локализацию текущего backend-контракта. */
public class UiTextTest {
    @Test
    public void localizesRoles() {
        assertEquals("Администратор", UiText.role(ApiModels.Role.ADMIN));
        assertEquals("Преподаватель", UiText.role(ApiModels.Role.TEACHER));
        assertEquals("Наблюдатель", UiText.role(ApiModels.Role.VIEWER));
        assertEquals("Администратор, Преподаватель",
                UiText.roles(EnumSet.of(ApiModels.Role.ADMIN, ApiModels.Role.TEACHER)));
    }

    @Test
    public void localizesNotApplicableRisk() {
        assertEquals("Не применяется", UiText.risk(ApiModels.RiskLevel.NOT_APPLICABLE));
    }

    @Test
    public void formatsHundredPointGradeWithoutRedundantZeroes() {
        assertEquals("68.5", UiText.grade(new BigDecimal("68.50")));
        assertEquals("100", UiText.grade(new BigDecimal("100.00")));
    }
}
