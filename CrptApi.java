package com.jober.fairmark;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import com.google.gson.Gson;
import java.util.Base64;

import java.util.concurrent.*;

@Slf4j
public class CrptApi {
    private final Semaphore semaphore;

    /**
     * Конструктор, принимающий ограничения для выполнения запросов на создание документа
     *
     * @param timeUnit промежуток времени для отсчёта – секунда, минута и пр
     * @param requestLimit положительное значение, которое определяет
     *                    максимальное количество запросов в этом промежутке времени.
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        log.info("Creating CrptApi with timeUnit: " + timeUnit + " and requestLimit: " + requestLimit);
        this.semaphore = new Semaphore(requestLimit);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(semaphore::release, 1, timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
    }

    /**
     *
     * @param document Документ в виде Java-объекта (не base64)
     * @param signature предполагаем что подпись поступает в base64
     */
    public void createDocument(Object document, String signature) {
        log.info("Trying to acquire Semaphore");
        semaphore.acquireUninterruptibly();
        try {
            log.info("Trying to create Document: " + document + " with signature: " + signature);
            Gson gson = new Gson();
            String docJson = gson.toJson(document);
            String docBase64 = Base64.getEncoder().encodeToString(docJson.getBytes());
            String url = "https://markirovka.demo.crpt.tech/api/v1/outgoing-documents";
            String madeInRussia = "LP_INTRODUCE_GOODS";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer your_token_here");

            String requestJson = "{\"document_format\": " + "MANUAL"
                    + ", \"product_document\": \"" + docBase64
                    + ", \"signature\": \"" + signature
                    + ", \"type\": \"" + madeInRussia
                    + "\"}";

            HttpEntity<Object> entity = new HttpEntity<>(requestJson, headers);
            String response = restTemplate.postForObject(url, entity, String.class);
            log.info("Response from API: " + response);
        } finally {
            log.info("Releasing Semaphore");
            semaphore.release();
        }
    }
}
