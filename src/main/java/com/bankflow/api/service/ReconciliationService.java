package com.bankflow.api.service;

import com.bankflow.api.dto.ReconciliationDtos.AccountReconciliationIssue;
import com.bankflow.api.dto.ReconciliationDtos.ReconciliationReport;
import com.bankflow.api.dto.ReconciliationDtos.TransferReconciliationIssue;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {
    private final JdbcTemplate jdbcTemplate;

    public ReconciliationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile() {
        List<AccountReconciliationIssue> accountIssues = jdbcTemplate.query("""
            select a.id,
                   a.balance as account_balance,
                   coalesce(sum(case when le.entry_type = 'CREDIT' then le.amount else -le.amount end), 0) as ledger_balance
            from accounts a
            left join ledger_entries le on le.account_id = a.id
            group by a.id, a.balance
            having a.balance <> coalesce(sum(case when le.entry_type = 'CREDIT' then le.amount else -le.amount end), 0)
            order by a.id
            """, (rs, rowNum) -> accountIssue(rs));

        List<TransferReconciliationIssue> transferIssues = jdbcTemplate.query("""
            select t.id,
                   coalesce(sum(case when le.entry_type = 'DEBIT' then le.amount else 0 end), 0) as debits,
                   coalesce(sum(case when le.entry_type = 'CREDIT' then le.amount else 0 end), 0) as credits,
                   count(le.id) as entry_count
            from transfers t
            join ledger_entries le on le.transfer_id = t.id
            where t.source_account_id is not null and t.destination_account_id is not null
            group by t.id
            having coalesce(sum(case when le.entry_type = 'DEBIT' then le.amount else 0 end), 0)
                 <> coalesce(sum(case when le.entry_type = 'CREDIT' then le.amount else 0 end), 0)
                or count(le.id) <> 2
            order by t.id
            """, (rs, rowNum) -> transferIssue(rs));

        return new ReconciliationReport(Instant.now(), accountIssues.isEmpty() && transferIssues.isEmpty(), accountIssues, transferIssues);
    }

    private AccountReconciliationIssue accountIssue(ResultSet rs) throws SQLException {
        BigDecimal accountBalance = rs.getBigDecimal("account_balance");
        BigDecimal ledgerBalance = rs.getBigDecimal("ledger_balance");
        return new AccountReconciliationIssue(
            rs.getObject("id", UUID.class),
            accountBalance,
            ledgerBalance,
            accountBalance.subtract(ledgerBalance)
        );
    }

    private TransferReconciliationIssue transferIssue(ResultSet rs) throws SQLException {
        return new TransferReconciliationIssue(
            rs.getObject("id", UUID.class),
            rs.getBigDecimal("debits"),
            rs.getBigDecimal("credits"),
            rs.getLong("entry_count")
        );
    }
}
