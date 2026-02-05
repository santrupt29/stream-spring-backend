package com.stream.app;

import com.stream.app.services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SpringStreamBackendApplicationTests {

	@Autowired
	VideoService videoService;
	@Test
	void contextLoads() {
		videoService.processVideo("405d49bf-c00b-438a-a5c5-7e5416c6aba5");
	}

}
