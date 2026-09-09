package am.ik.spring.batch.dashboard.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import am.ik.spring.batch.dashboard.config.BatchTableNames;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JobInstanceMapper {

	private final JdbcClient jdbcClient;

	private final BatchTableNames tableNames;

	public JobInstanceMapper(JdbcClient jdbcClient, BatchTableNames tableNames) {
		this.jdbcClient = jdbcClient;
		this.tableNames = tableNames;
	}

	public PageResponse<JobInstance> findJobInstances(JobInstancesParams params) {
		Integer page = Objects.requireNonNullElse(params.page(), 0);
		Integer size = Objects.requireNonNullElse(params.size(), 20);
		List<JobInstance> content = this.jdbcClient
			.sql("""
					SELECT
					    ji.JOB_INSTANCE_ID,
					    ji.JOB_NAME,
					    ji.JOB_KEY,
					    ji.VERSION,
					    je.JOB_EXECUTION_ID,
					    je.START_TIME,
					    je.END_TIME,
					    je.STATUS
					FROM
					    %s ji
					    LEFT JOIN
					        (
					            -- Subquery to get the latest execution for each job instance
					            SELECT
					                je1.*
					            FROM
					                %s je1
					                JOIN
					                    (
					                        SELECT
					                            JOB_INSTANCE_ID,
					                            MAX(JOB_EXECUTION_ID) as MAX_EXECUTION_ID
					                        FROM
					                            %s
					                        GROUP BY
					                            JOB_INSTANCE_ID
					                    ) je2
					                ON  je1.JOB_EXECUTION_ID = je2.MAX_EXECUTION_ID
					        ) je
					    ON  ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
					WHERE
					    (
					        :jobName::VARCHAR IS NULL
					    OR  ji.JOB_NAME = :jobName
					    )
					ORDER BY
					    ji.JOB_INSTANCE_ID DESC
					LIMIT :size OFFSET :page * :size
					""".formatted(this.tableNames.jobInstance(), this.tableNames.jobExecution(),
					this.tableNames.jobExecution()))
			.param("jobName", params.jobName())
			.param("page", page)
			.param("size", size)
			.query((rs, rowNum) -> JobInstanceBuilder.jobInstance()
				.jobInstanceId(rs.getLong("JOB_INSTANCE_ID"))
				.jobName(rs.getString("JOB_NAME"))
				.jobKey(rs.getString("JOB_KEY"))
				.version(rs.getInt("VERSION"))
				.latestExecution(JobExecutionSummaryBuilder.jobExecutionSummary()
					.jobExecutionId(rs.getLong("JOB_EXECUTION_ID"))
					.startTime(rs.getObject("START_TIME", LocalDateTime.class))
					.endTime(rs.getObject("END_TIME", LocalDateTime.class))
					.status(JobStatus.valueOf(rs.getString("STATUS")))
					.build())
				.build())
			.list();

		long count = this.jdbcClient.sql("""
				SELECT COUNT(*)
				FROM %s ji
				WHERE (:jobName::VARCHAR IS NULL OR ji.JOB_NAME = :jobName)
				""".formatted(this.tableNames.jobInstance()))
			.param("jobName", params.jobName())
			.query(Long.class)
			.single();

		return PageResponseBuilder.<JobInstance>pageResponse()
			.content(content)
			.page(page)
			.size(size)
			.totalElements(count)
			.totalPages((int) (count / size) + 1)
			.build();
	}

	public Optional<JobInstanceDetail> getJobInstanceDetail(long jobInstanceId) {
		return this.jdbcClient.sql("""
				SELECT
				    ji.JOB_INSTANCE_ID,
				    ji.JOB_NAME,
				    ji.JOB_KEY,
				    ji.VERSION,
				    je.JOB_EXECUTION_ID,
				    je.CREATE_TIME,
				    je.START_TIME,
				    je.END_TIME,
				    je.STATUS,
				    je.EXIT_CODE,
				    je.EXIT_MESSAGE,
				    je.LAST_UPDATED
				FROM
				    %s ji
				    JOIN
				        %s je
				    ON  ji.JOB_INSTANCE_ID = je.JOB_INSTANCE_ID
				WHERE
				    ji.JOB_INSTANCE_ID = :jobInstanceId
				""".formatted(this.tableNames.jobInstance(), this.tableNames.jobExecution()))
			.param("jobInstanceId", jobInstanceId)
			.query((rs, rowNum) -> JobInstanceDetailBuilder.jobInstanceDetail()
				.jobInstanceId(rs.getLong("JOB_INSTANCE_ID"))
				.jobName(rs.getString("JOB_NAME"))
				.jobKey(rs.getString("JOB_KEY"))
				.version(rs.getInt("VERSION"))
				.latestExecution(JobExecutionSummaryBuilder.jobExecutionSummary()
					.jobExecutionId(rs.getLong("JOB_EXECUTION_ID"))
					.startTime(rs.getObject("START_TIME", LocalDateTime.class))
					.endTime(rs.getObject("END_TIME", LocalDateTime.class))
					.status(JobStatus.valueOf(rs.getString("STATUS")))
					.build())
				.executions(List.of())
				.build())
			.optional()
			.map(jobInstanceDetail -> JobInstanceDetailBuilder.from(jobInstanceDetail)
				.executions(this.jdbcClient.sql("""
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
						    je.JOB_INSTANCE_ID = :jobInstanceId
						ORDER BY
						    je.START_TIME DESC
						""".formatted(this.tableNames.jobExecution(), this.tableNames.jobInstance()))
					.param("jobInstanceId", jobInstanceId)
					.query(JobExecution.class)
					.list())
				.build());
	}

}
