package ua.kharkiv.elsol.exporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Exporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(Exporter.class);

  public static void main(String[] args) {
    LOGGER.info("STARTING THE APPLICATION");
    SpringApplication.run(Exporter.class, args);
    LOGGER.info("APPLICATION FINISHED");
  }

}
