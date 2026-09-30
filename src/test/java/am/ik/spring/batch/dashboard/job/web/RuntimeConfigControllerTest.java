package am.ik.spring.batch.dashboard.job.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RuntimeConfigControllerTest {

	@Test
	void should_return_runtime_context_path() {
		RuntimeConfigController controller = new RuntimeConfigController();
		HttpServletRequest request = new MockHttpServletRequest("GET",
				"/admin/groen/batch-dashboard/runtime-config.js");
		((MockHttpServletRequest) request).setContextPath("/admin/groen/batch-dashboard");

		String config = controller.runtimeConfig(request);

		assertThat(config)
			.isEqualTo("window.__SPRING_BATCH_DASHBOARD_CONTEXT_PATH__ = \"/admin/groen/batch-dashboard\";");
	}

}
