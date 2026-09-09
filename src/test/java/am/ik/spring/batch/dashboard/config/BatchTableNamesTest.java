package am.ik.spring.batch.dashboard.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchTableNamesTest {

	@Test
	void shouldUseDefaultSpringBatchTableNamesWhenPrefixIsEmpty() {
		BatchTableNames tableNames = new BatchTableNames("");

		assertThat(tableNames.jobExecution()).isEqualTo("BATCH_JOB_EXECUTION");
		assertThat(tableNames.jobExecutionParams()).isEqualTo("BATCH_JOB_EXECUTION_PARAMS");
		assertThat(tableNames.jobInstance()).isEqualTo("BATCH_JOB_INSTANCE");
		assertThat(tableNames.stepExecution()).isEqualTo("BATCH_STEP_EXECUTION");
	}

	@Test
	void shouldPrependConfiguredPrefixToSpringBatchTableNames() {
		BatchTableNames tableNames = new BatchTableNames("CUSTOM_");

		assertThat(tableNames.jobExecution()).isEqualTo("CUSTOM_BATCH_JOB_EXECUTION");
		assertThat(tableNames.jobExecutionParams()).isEqualTo("CUSTOM_BATCH_JOB_EXECUTION_PARAMS");
		assertThat(tableNames.jobInstance()).isEqualTo("CUSTOM_BATCH_JOB_INSTANCE");
		assertThat(tableNames.stepExecution()).isEqualTo("CUSTOM_BATCH_STEP_EXECUTION");
	}

	@Test
	void shouldRejectUnsafeTablePrefix() {
		assertThat(
				org.assertj.core.api.Assertions.catchThrowable(() -> new BatchTableNames("CUSTOM_; DROP TABLE users;")))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
