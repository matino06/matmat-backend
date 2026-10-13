package com.example.EduSprint.service;

import com.example.EduSprint.config.AppTime;
import com.example.EduSprint.dto.QuotaUsageDTO;
import com.example.EduSprint.entity.Account;
import com.example.EduSprint.repository.AccountRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

// Mjesečna kvota kreatora: zbroj stvarnog troška izrade (build_cost) u kalendarskom mjesecu
// po hrvatskom vremenu, prema account.monthly_quota_usd. Varijacije zadataka se ne broje.
// U transakciji: zahtjev je račun već dohvatio preko JPA-a (open-in-view drži tu vezu do kraja zahtjeva),
// pa JdbcTemplate mora koristiti istu vezu; bez transakcije bi tražio drugu i zahtjevi bi se pod opterećenjem
// međusobno blokirali čekajući slobodnu vezu.
@Service
@Transactional
public class BuildQuotaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final AccountRepository accountRepository;

    public BuildQuotaService(NamedParameterJdbcTemplate jdbc, AccountRepository accountRepository) {
        this.jdbc = jdbc;
        this.accountRepository = accountRepository;
    }

    public BigDecimal spentThisMonth(long accountId) {
        BigDecimal spent = jdbc.queryForObject("""
                SELECT COALESCE(SUM(cost_usd), 0) FROM build_cost
                WHERE account_id = :a AND counts_quota AND created_at >= :from""",
                new MapSqlParameterSource("a", accountId).addValue("from", Timestamp.from(monthStart(AppTime.today()))),
                BigDecimal.class);
        return spent == null ? BigDecimal.ZERO : spent;
    }

    public boolean hasQuota(long accountId) {
        BigDecimal quota = accountRepository.findById(accountId).map(Account::getMonthlyQuotaUsd).orElse(BigDecimal.ZERO);
        return hasQuota(spentThisMonth(accountId), quota);
    }

    public QuotaUsageDTO usage(Account account) {
        return new QuotaUsageDTO(usedPercent(spentThisMonth(account.getAccountId()), account.getMonthlyQuotaUsd()),
                resetsOn(AppTime.today()));
    }

    static boolean hasQuota(BigDecimal spent, BigDecimal quota) {
        return quota != null && spent.compareTo(quota) < 0;
    }

    // Postotak za prikaz: zaokružen naniže, najviše 100 (prekoračenje zadnjim korakom se ne prikazuje).
    static int usedPercent(BigDecimal spent, BigDecimal quota) {
        if (quota == null || quota.signum() <= 0) return 100;
        int percent = spent.multiply(BigDecimal.valueOf(100)).divide(quota, 0, RoundingMode.DOWN).intValue();
        return Math.min(100, Math.max(0, percent));
    }

    static Instant monthStart(LocalDate today) {
        return today.withDayOfMonth(1).atStartOfDay(AppTime.ZONE).toInstant();
    }

    static LocalDate resetsOn(LocalDate today) {
        return today.withDayOfMonth(1).plusMonths(1);
    }
}
