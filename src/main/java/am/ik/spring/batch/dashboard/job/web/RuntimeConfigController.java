package am.ik.spring.batch.dashboard.job.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuntimeConfigController {

	@GetMapping(path = "/runtime-config.js", produces = "application/javascript")
	public String runtimeConfig(HttpServletRequest request) {
		return "window.__SPRING_BATCH_DASHBOARD_CONTEXT_PATH__ = \"" + request.getContextPath() + "\";";
	}

}
