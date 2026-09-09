package am.ik.spring.batch.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchTableNames {

	private static final String TABLE_PREFIX_PATTERN = "[A-Za-z_][A-Za-z0-9_$]*";

	private final String prefix;

	public BatchTableNames(@Value("${spring.batch.jdbc.table-prefix:}") String prefix) {
		if (!prefix.isEmpty() && !prefix.matches(TABLE_PREFIX_PATTERN)) {
			throw new IllegalArgumentException("spring.batch.jdbc.table-prefix must be a SQL identifier prefix");
		}
		this.prefix = prefix;
	}

	public String jobExecution() {
		return this.prefix + "BATCH_JOB_EXECUTION";
	}

	public String jobExecutionParams() {
		return this.prefix + "BATCH_JOB_EXECUTION_PARAMS";
	}

	public String jobInstance() {
		return this.prefix + "BATCH_JOB_INSTANCE";
	}

	public String stepExecution() {
		return this.prefix + "BATCH_STEP_EXECUTION";
	}

}
