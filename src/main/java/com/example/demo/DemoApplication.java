package com.example.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public ApplicationRunner runner(RestTemplate restTemplate) {
		return new ApplicationRunner() {

			final Map<String, String> generateBody = Map.of(
					"name", "Keerthi",
					"regNo", "PES2UG22CS136",
					"email", "john@example.com");

			final String GENERATE_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
			final String FALLBACK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

			@Override
			public void run(ApplicationArguments args) {

				System.out.println("Sending POST to generateWebhook...");

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				HttpEntity<Map<String, String>> request = new HttpEntity<>(generateBody, headers);

				ResponseEntity<Map> response = restTemplate.postForEntity(GENERATE_URL, request, Map.class);
				Map<String, Object> body = response.getBody();

				String webhook = (String) body.get("webhook");
				String token = (String) body.get("accessToken");

				System.out.println("Webhook: " + webhook);
				System.out.println("Access Token: " + mask(token));

				int lastTwo = extractLastTwoDigits(generateBody.get("regNo"));
				boolean isOdd = lastTwo % 2 == 1;
				String finalQuery = isOdd ? sql1() : sql2();

				System.out.println("Using query for " + (isOdd ? "Question 1 (odd)" : "Question 2 (even)"));

				String submitUrl = (webhook != null && !webhook.isBlank()) ? webhook : FALLBACK_URL;

				Map<String, String> submitBody = new HashMap<>();
				submitBody.put("finalQuery", finalQuery);

				HttpHeaders submitHeaders = new HttpHeaders();
				submitHeaders.setContentType(MediaType.APPLICATION_JSON);
				submitHeaders.set("Authorization", "Bearer " + token);

				HttpEntity<Map<String, String>> submitReq = new HttpEntity<>(submitBody, submitHeaders);
				ResponseEntity<String> submitResp = restTemplate.postForEntity(submitUrl, submitReq, String.class);

				System.out.println("Submit Response: " + submitResp.getStatusCode());
				System.out.println("Body: " + submitResp.getBody());
			}

			private String sql1() {
				return """
						SELECT customer_id, SUM(amount) AS total_amount
						FROM transactions
						GROUP BY customer_id
						ORDER BY total_amount DESC
						LIMIT 10;
						""";
			}

			private String sql2() {
				return """
						SELECT product_id, AVG(rating) AS avg_rating
						FROM reviews
						GROUP BY product_id
						HAVING COUNT(*) >= 5
						ORDER BY avg_rating DESC;
						""";
			}

			private int extractLastTwoDigits(String regNo) {
				String digits = regNo.replaceAll("\\D", "");
				if (digits.isEmpty())
					return 0;
				return Integer.parseInt(digits.substring(Math.max(0, digits.length() - 2)));
			}

			private String mask(String token) {
				if (token == null)
					return "null";
				if (token.length() <= 10)
					return "****";
				return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
			}
		};
	}
}
