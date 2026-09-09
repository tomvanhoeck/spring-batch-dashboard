package am.ik.spring.batch.dashboard.job;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import am.ik.spring.batch.dashboard.config.BatchTableNames;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JobExecutionMapper {

	private final JdbcClient jdbcClient;

	private final BatchTableNames tableNames;

	public JobExecutionMapper(JdbcClient jdbcClient, BatchTableNames tableNames) {
		this.jdbcClient = jdbcClient;
		this.tableNames = tableNames;
	}

	public PageResponse<JobExecution> findJobExecutions(JobExecutionsParams params) {
		Integer page = Objects.requireNonNullElse(params.page(), 0);
		Integer size = Objects.requireNonNullElse(params.size(), 20);
		List<JobExecution> content = this.jdbcClient.sql("""
				SELECT
				    je.JOB_EXECUTION_ID,
				    je.JOB_INSTANCE_ID,
				    ji.JOB_NAME,
				    je.CREATE_TIME,
				    je.START_TIME,
				    je.END_TIME,
				    je.STATUS,
				    je.EXIT_CODE,
				    je.EXIT_MESSAGE
				FROM
				    %s je
				    JOIN
				        %s ji
				    ON  je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
				WHERE
				    (
				        :jobName::VARCHAR IS NULL
				    OR  ji.JOB_NAME = :jobName
				    )
				AND (
				        :status::VARCHAR IS NULL
				    OR  je.STATUS = :status
				    )
				AND (
				        :startDateFrom::TIMESTAMP IS NULL
				    OR  je.START_TIME >= :startDateFrom
				    )
				AND (
				        :startDateTo::TIMESTAMP IS NULL
				    OR  je.START_TIME <= :startDateTo
				    )
				ORDER BY
				    je.START_TIME DESC
				LIMIT :size OFFSET :page * :size
				""".formatted(this.tableNames.jobExecution(), this.tableNames.jobInstance()))
			.param("jobName", params.jobName())
			.param("status", params.status(), Types.VARCHAR)
			.param("startDateFrom", params.startDateFrom())
			.param("startDateTo", params.startDateTo())
			.param("page", page)
			.param("size", size)
			.query(JobExecution.class)
			.list();
		long count = this.jdbcClient.sql("""
				SELECT
				    COUNT(*)
				FROM
				    %s je
				    JOIN
				        %s ji
				    ON  je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
				WHERE
				    (
				        :jobName::VARCHAR IS NULL
				    OR  ji.JOB_NAME = :jobName
				    )
				AND (
				        :status::VARCHAR IS NULL
				    OR  je.STATUS = :status
				    )
				AND (
				        :startDateFrom::TIMESTAMP IS NULL
				    OR  je.START_TIME >= :startDateFrom
				    )
				AND (
				        :startDateTo::TIMESTAMP IS NULL
				    OR  je.START_TIME <= :startDateTo
				    )
				""".formatted(this.tableNames.jobExecution(), this.tableNames.jobInstance()))
			.param("jobName", params.jobName())
			.param("status", params.status(), Types.VARCHAR)
			.param("startDateFrom", params.startDateFrom())
			.param("startDateTo", params.startDateTo())
			.query(Long.class)
			.single();
		return PageResponseBuilder.<JobExecution>pageResponse()
			.content(content)
			.page(page)
			.size(size)
			.totalElements(count)
			.totalPages((int) (count / size) + 1)
			.build();
	}

	public Optional<JobExecutionDetail> getJobExecutionDetail(long jobExecutionId) {
		Optional<JobExecutionDetail> jobExecutionDetail = jdbcClient.sql("""
				SELECT
				    je.JOB_EXECUTION_ID,
				    je.JOB_INSTANCE_ID,
				    ji.JOB_NAME,
				    je.CREATE_TIME,
				    je.START_TIME,
				    je.END_TIME,
				    je.STATUS,
				    je.EXIT_CODE,
				    je.EXIT_MESSAGE,
				    je.LAST_UPDATED
				FROM
				    %s je
				    JOIN
				        %s ji
				    ON  je.JOB_INSTANCE_ID = ji.JOB_INSTANCE_ID
				WHERE
				    je.JOB_EXECUTION_ID = :jobExecutionId
				""".formatted(this.tableNames.jobExecution(), this.tableNames.jobInstance()))
			.param("jobExecutionId", jobExecutionId)
			.query((rs, rowNum) -> JobExecutionDetailBuilder.jobExecutionDetail()
				.jobExecutionId(rs.getLong("JOB_EXECUTION_ID"))
				.jobInstanceId(rs.getLong("JOB_INSTANCE_ID"))
				.jobName(rs.getString("JOB_NAME"))
				.createTime(rs.getObject("CREATE_TIME", LocalDateTime.class))
				.startTime(rs.getObject("START_TIME", LocalDateTime.class))
				.endTime(rs.getObject("END_TIME", LocalDateTime.class))
				.status(JobStatus.valueOf(rs.getString("STATUS")))
				.exitCode(rs.getString("EXIT_CODE"))
				.exitMessage(rs.getString("EXIT_MESSAGE"))
				.lastUpdated(rs.getObject("LAST_UPDATED", LocalDateTime.class))
				.parameters(List.of())
				.steps(List.of())
				.build())
			.optional();
		return jobExecutionDetail.map(je -> {
			List<JobParameter> jobParameters = this.jdbcClient.sql("""
					SELECT
					    jp.PARAMETER_NAME AS NAME,
					    jp.PARAMETER_TYPE AS TYPE,
					    jp.PARAMETER_VALUE AS VALUE,
					    jp.IDENTIFYING
					FROM
					    %s jp
					WHERE
					    jp.JOB_EXECUTION_ID = :jobExecutionId
					ORDER BY
					    jp.PARAMETER_NAME ASC
					""".formatted(this.tableNames.jobExecutionParams()))
				.param("jobExecutionId", jobExecutionId)
				.query(JobParameter.class)
				.list();
			List<StepExecutionSummary> stepExecutions = this.jdbcClient.sql("""
					SELECT
					    se.STEP_EXECUTION_ID,
					    se.STEP_NAME,
					    se.STATUS,
					    se.READ_COUNT,
					    se.WRITE_COUNT,
					    se.FILTER_COUNT,
					    se.START_TIME,
					    se.END_TIME
					FROM
					    %s se
					WHERE
					    se.JOB_EXECUTION_ID = :jobExecutionId
					ORDER BY
					    se.START_TIME DESC
					""".formatted(this.tableNames.stepExecution()))
				.param("jobExecutionId", jobExecutionId)
				.query(StepExecutionSummary.class)
				.list();
			return JobExecutionDetailBuilder.from(je).parameters(jobParameters).steps(stepExecutions).build();
		});
	}

}
