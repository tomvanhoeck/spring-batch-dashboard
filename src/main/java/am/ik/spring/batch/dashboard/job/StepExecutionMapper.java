package am.ik.spring.batch.dashboard.job;

import java.util.Optional;
import am.ik.spring.batch.dashboard.config.BatchTableNames;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StepExecutionMapper {

	private final JdbcClient jdbcClient;

	private final BatchTableNames tableNames;

	public StepExecutionMapper(JdbcClient jdbcClient, BatchTableNames tableNames) {
		this.jdbcClient = jdbcClient;
		this.tableNames = tableNames;
	}

	public Optional<StepExecutionDetail> getStepExecutionDetail(long stepExecutionId) {
		return this.jdbcClient
			.sql("""
					SELECT
					    se.STEP_EXECUTION_ID,
					    se.JOB_EXECUTION_ID,
					    se.STEP_NAME,
					    se.VERSION,
					    se.CREATE_TIME,
					    se.START_TIME,
					    se.END_TIME,
					    se.STATUS,
					    se.COMMIT_COUNT,
					    se.READ_COUNT,
					    se.FILTER_COUNT,
					    se.WRITE_COUNT,
					    se.READ_SKIP_COUNT,
					    se.WRITE_SKIP_COUNT,
					    se.PROCESS_SKIP_COUNT,
					    se.ROLLBACK_COUNT,
					    se.EXIT_CODE,
					    se.EXIT_MESSAGE,
					    se.LAST_UPDATED,
					    je.CREATE_TIME AS JOB_CREATE_TIME,
					    je.STATUS AS JOB_STATUS,
					    ji.JOB_NAME
					FROM
					    %s se
					    JOIN
					    %s je
					    ON  se.JOB_EXECUTION_ID = je.JOB_EXECUTION_ID
					    JOIN
					        %s ji
					    ON  je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
					WHERE
					    se.STEP_EXECUTION_ID = :stepExecutionId
					ORDER BY
					    se.END_TIME DESC
					""".formatted(this.tableNames.stepExecution(), this.tableNames.jobExecution(),
					this.tableNames.jobInstance()))
			.param("stepExecutionId", stepExecutionId)
			.query(StepExecutionDetail.class)
			.optional();
	}

}
