package egovframework.example.sample.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import egovframework.example.API.Keys;

@RestController
public class RestAPIController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);

	@GetMapping("/{name}.do")
	public String sayHello(@PathVariable String name) {
		String result = "Hello eGovFramework!! name : " + name;
		return result;
	}

	private static String getPythonPath() throws IOException {
		ProcessBuilder whereProcessBuilder = new ProcessBuilder("which", "python");
		Process whereProcess = whereProcessBuilder.start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(whereProcess.getInputStream()))) {
			String line;
			if ((line = reader.readLine()) != null) {
				return line.trim();
			}
		}

		return null;
	}

	@PostMapping("/install-guide.do")
	@ResponseBody
	public ResponseEntity<?> installGuide(HttpServletRequest request) throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();

		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");

		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변
		ObjectMapper objectMapper = new ObjectMapper();

		ServletContext context = request.getSession().getServletContext();
		String projectPath = context.getRealPath("/");

		OSDetect osd = new OSDetect(projectPath);
		osd.whisperDetection();

		Path resource_path = Paths.get(osd.getResource_address());
		if (!Files.exists(resource_path)) {
			logger.error("resource 폴더가 존재하지 않습니다. resource 폴더를 다운받아 주세요.");
			logger.error("resource 폴더를 둘 곳: " + osd.getResource_address());

			response.put("Install the 'resource' folder at the following address: ", osd.getResource_address());
			// ObjectMapper objectMapper = new ObjectMapper();
			String jsonResponse = objectMapper.writeValueAsString(response);
			return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
		}

		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response.put("executionTimeInMilli", Long.toString(executionTime));
		logger.info("Execution time:" + executionTime);
		String jsonResponse = objectMapper.writeValueAsString(response);

		return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
	}

	@PostMapping("/post-test.do")
	@ResponseBody
	public ResponseEntity<?> postTest(HttpServletRequest request) throws IOException, InterruptedException {

		long startTime = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();

		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");

		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변
		ObjectMapper objectMapper = new ObjectMapper();

		ProcessBuilder pyProcessBuilder = new ProcessBuilder();
		pyProcessBuilder.command("bash", "-c", "which python3");

		Process process = pyProcessBuilder.start();
		logger.debug("pyProcessBuilder.start()");

		StringBuilder python3_loc = new StringBuilder();

		Thread outputThread = new Thread(() -> {
			try {
				InputStream inputStream = process.getInputStream();
				InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String python3_loc_line;
				python3_loc_line = bufferedReader.readLine();
				python3_loc.append(python3_loc_line);

			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		outputThread.start();

		int exitCode;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
			exitCode = -1;
		}

		outputThread.join();
		logger.debug("python3_loc:" + python3_loc);
		logger.debug("Extract process exited with code: " + exitCode);
		String replaceTargetString = "#!" + python3_loc;

		ServletContext context = request.getSession().getServletContext();
		String projectPath = context.getRealPath("/");
		String whisper_addr = projectPath + "resources/mac/whisper/bin/whisper";
		StringBuilder content = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(whisper_addr))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			logger.debug("Origin whisper content:\n" + content);
			if (content.length() > 0) {
				String firstLine = content.toString().split("\n")[0];
				content.replace(0, firstLine.length(), replaceTargetString);
			}
			logger.debug("Modified whisper content:\n" + content);

			// 변경된 내용을 다시 파일에 쓰기
			try (PrintWriter writer = new PrintWriter(new FileWriter(whisper_addr))) {
				writer.print(content);
			} catch (IOException e) {
				logger.error("Error writing to the 'whisper' file.", e);
			}
		} catch (IOException e) {
			logger.error("No whisper", e);
		}

		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response.put("executionTimeInMilli", Long.toString(executionTime));
		logger.info("Execution time:" + executionTime);
		String jsonResponse = objectMapper.writeValueAsString(response);

		return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
	}

	// 회의록 작성 기능
	@PostMapping("/minutes.do")
	@ResponseBody
	public ResponseEntity<?> extractTimestamp(@RequestParam MultipartFile file,
			@RequestParam(name = "ppl", required = false) String ppl, HttpServletRequest request)
			throws IOException, InterruptedException {
		OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

        ServletContext context = request.getSession().getServletContext();
        String projectPath = context.getRealPath("/");
        String absolutePathString = "";

        
        logger.debug("projectPath: " + projectPath);
        if(ppl!=null) {
           logger.debug("lang: " + ppl);
        }

        /* OS detection */
        OSDetect osd = new OSDetect(projectPath);
        
        
        FileController fc = new FileController(response, file, osd);
        fc.exist();
        response = fc.sizing();
		
        logger.debug("Project Path: " + projectPath);

        absolutePathString = fc.setAbsolutePath();
        String origin_absolutePathString = new String(absolutePathString);
        logger.debug("AbsolutePathString received: " + absolutePathString);
        
        File extractedAudio = null;
        extractedAudio = fc.runFfmpeg(extractedAudio);
        if (extractedAudio != null && extractedAudio.length() > 26214400) {
       	 return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
					HttpStatus.OK);
        }


		

		String falskUrl = "http://172.17.200.193:8888/take-minutes";

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters()
	    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

		// JSON 객체 생성
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode requestBody = mapper.createObjectNode();
		requestBody.put("absolutePathString", absolutePathString);
		requestBody.put("ppl", ppl);

		

		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

		ResponseEntity<String> responseEntity = restTemplate.postForEntity(falskUrl, requestEntity, String.class);
		
		String noteResult = responseEntity.getBody();
		
		
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		logger.info("Execution time:" + executionTime);
		logger.debug(responseEntity);
		
		Result rslt = new Result();
		response = rslt.getResult(response, fc.getFile_size(), noteResult, executionTime);



		fc.deleteFile(origin_absolutePathString);

		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}

	//회의록 요약기능
	@PostMapping("/minutes-summary.do")
	@ResponseBody
	public ResponseEntity<?> MinuteSummary(@RequestParam MultipartFile file,
			@RequestParam(name = "ppl", required = false) String ppl, HttpServletRequest request)
			throws IOException, InterruptedException {
		OpenAiService service = new OpenAiService(Keys.OPENAPI_KEY, Duration.ofMinutes(9999));
        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

        ServletContext context = request.getSession().getServletContext();
        String projectPath = context.getRealPath("/");
        String absolutePathString = "";

        
        logger.debug("projectPath: " + projectPath);
        if(ppl!=null) {
           logger.debug("lang: " + ppl);
        }

        /* OS detection */
        OSDetect osd = new OSDetect(projectPath);
        
        
        FileController fc = new FileController(response, file, osd);
        fc.exist();
        response = fc.sizing();
		
        logger.debug("Project Path: " + projectPath);

        absolutePathString = fc.setAbsolutePath();
        String origin_absolutePathString = new String(absolutePathString);
        logger.debug("AbsolutePathString received: " + absolutePathString);
        
        File extractedAudio = null;
        extractedAudio = fc.runFfmpeg(extractedAudio);
        if (extractedAudio != null && extractedAudio.length() > 26214400) {
       	 return new ResponseEntity<>("오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.", headers,
					HttpStatus.OK);
        }


		

		String falskUrl = "http://172.17.200.193:8888/take-minutes";

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters()
	    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

		// JSON 객체 생성
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode requestBody = mapper.createObjectNode();
		requestBody.put("absolutePathString", absolutePathString);
		requestBody.put("ppl", ppl);

		

		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

		ResponseEntity<String> responseEntity = restTemplate.postForEntity(falskUrl, requestEntity, String.class);
		
		String noteResult = responseEntity.getBody();
		
		List<ChatMessage> message = new ArrayList<ChatMessage>();
		message.add(new ChatMessage("user", "다음 회의록을 700token 이하로 요약해. 잘하면 상을 줄게" + noteResult + "\""));
		
		ChatCompletionRequest completionRequest = ChatCompletionRequest.builder().messages(message)
				.model("gpt-3.5-turbo-16k")
				.maxTokens(700).temperature((double) 0.5f).build();
		String summary_result = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage()
				.getContent();
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		logger.info("Execution time:" + executionTime);
		logger.debug(responseEntity);
		
		Result rslt = new Result();
		response = rslt.getResult(response, fc.getFile_size(), summary_result, executionTime);
		


		fc.deleteFile(origin_absolutePathString);

		return new ResponseEntity<>(response, headers, HttpStatus.OK);
	}
}
