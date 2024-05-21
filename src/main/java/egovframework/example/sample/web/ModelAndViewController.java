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
import org.springframework.stereotype.Controller;
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
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import egovframework.example.API.Keys;

@Controller
public class ModelAndViewController {
	private static final Logger logger = LogManager.getLogger(EgovSampleController.class);

	@PostMapping("summarize-vid-mnv.do")
	public ModelAndView summaryUsingWhisperMnV(@RequestParam("file") MultipartFile file, HttpServletRequest request)
			throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

		ServletContext context = request.getSession().getServletContext();
		String projectPath = context.getRealPath("/");
		String absolutePathString = "";
		logger.debug("projectPath: " + projectPath);

		/* OS detection */
		OSDetect osd = new OSDetect(projectPath);
		osd.detection();

		Path resource_path = Paths.get(osd.getResource_address());

		FileController fc = new FileController(response, file, osd);
		fc.exist();
		response = fc.sizing();

		logger.debug("Project Path: " + projectPath);

		absolutePathString = fc.setAbsolutePath();
		String origin_absolutePathString = new String(absolutePathString);
		logger.debug("AbsolutePathString received" + absolutePathString);

		File extractedAudio = null;
		/* ffmpeg */
		extractedAudio = fc.runFfmpeg(extractedAudio);
		if (extractedAudio != null && extractedAudio.length() > 26214400) {
			ModelAndView modelAndView = new ModelAndView();
			modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
			modelAndView.addObject("errorMessage", "오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.");
			return modelAndView;
		}

		WhisperController wc = new WhisperController();

		if (extractedAudio != null) {
			String extractedAudiFilePath = extractedAudio.getAbsolutePath();
			absolutePathString = extractedAudiFilePath;
		}
		wc.transcript(absolutePathString);

		fc.deleteFile(origin_absolutePathString);

		String str = "다음 텍스트의 주제를 파악해서 텍스트의 언어로 700token 이하로 요약해줘. 잘하면 상을 줄게: \"";
		String summary_result = wc.getResult(str);

		Result rslt = new Result();

		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response = rslt.getResult(response, fc.getFile_size(), summary_result, executionTime);
		logger.debug(summary_result);
		logger.info("Execution time:" + executionTime);


		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("result"); // 반환할 JSP 파일 경로 설정
		modelAndView.addObject("arkeeperresult", summary_result); // 결과 데이터를 모델에 추가

		return modelAndView;
	}
	
	@PostMapping("/extract-tag-mnv.do")
	public ModelAndView extractTagsUsingWhisperMnV(@RequestParam("file") MultipartFile file, HttpServletRequest request)
	        throws IOException, InterruptedException {
		long startTime = System.currentTimeMillis();
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
		Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

		ServletContext context = request.getSession().getServletContext();
		String projectPath = context.getRealPath("/");
		String absolutePathString = "";
		logger.debug("projectPath: " + projectPath);

		/* OS detection */
		OSDetect osd = new OSDetect(projectPath);
		osd.detection();
		
		Path resource_path = Paths.get(osd.getResource_address());
        if (!Files.exists(resource_path)) {
            logger.error("resource 폴더가 존재하지 않습니다. resource 폴더를 다운받아 주세요.");
            logger.error("resource 폴더를 둘 곳: "+ osd.getResource_address());
            
            response.put("Install the 'resource' folder at the following address: ", osd.getResource_address());
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = objectMapper.writeValueAsString(response);
            ModelAndView modelAndView = new ModelAndView();
		    modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
		    return modelAndView;
        }

		FileController fc = new FileController(response, file, osd);
		fc.exist();
		response = fc.sizing();
		
		logger.debug("Project Path: " + projectPath);
		
		absolutePathString = fc.setAbsolutePath();
		String origin_absolutePathString = new String(absolutePathString);
		
		logger.debug("AbsolutePathString received" + absolutePathString);
		
		File extractedAudio = null;
		/* ffmpeg */
		extractedAudio = fc.runFfmpeg(extractedAudio);
		if (extractedAudio != null && extractedAudio.length() > 26214400) {
			ModelAndView modelAndView = new ModelAndView();
		    modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
		    return modelAndView;
		}

		// 받아온 주소를 whisper에게 보내
		WhisperController wc = new WhisperController();
		if(extractedAudio!=null) {
			String extractedAudiFilePath = extractedAudio.getAbsolutePath();
			absolutePathString = extractedAudiFilePath;
		}
		wc.transcript(absolutePathString);

		fc.deleteFile(origin_absolutePathString);
		
		String str = "다음 텍스트의 주요한 태그를 중요한 순서대로 5개를 쉼표를 구분자로 사용해서 추출해줘. 잘하면 상을 줄게: \"";
		String summary_result = wc.getResult(str);

		Result rslt = new Result();
		
		long endTime = System.currentTimeMillis();
		long executionTime = endTime - startTime;
		response = rslt.getResult(response, fc.getFile_size(), summary_result, executionTime);
		logger.debug(summary_result);
		logger.info("Execution time:"+executionTime);


		ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("result"); // 반환할 JSP 파일 경로 설정
        modelAndView.addObject("arkeeperresult", summary_result); // 결과 데이터를 모델에 추가

        return modelAndView;
	}
	
	 @PostMapping("/timestamp-mnv.do")
     public ModelAndView extractTimestampMnV(
           @RequestParam MultipartFile file, 
           @RequestParam("searchfor") String searchfor, 
           @RequestParam(name = "lang", required = false) String lang,
           @RequestParam(name = "locOfPython", required = false) String locOfPython,
           HttpServletRequest request)
           throws IOException, InterruptedException {
        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
        Map<String, String> response = new HashMap<>();// 결과를 맵핑할 변

        ServletContext context = request.getSession().getServletContext();
        String projectPath = context.getRealPath("/");
        String absolutePathString = "";
        logger.debug("searchfor: " + searchfor);
        if (locOfPython == null||locOfPython.isEmpty()) {
       	    logger.debug("emptylocOfPython" );
       	    locOfPython = null;
       	}
        
        logger.debug("locOfPython: " + "\""+locOfPython+ "\"");
        
        logger.debug("projectPath: " + projectPath);
        if(lang!=null) {
           logger.debug("lang: " + lang);
        }

        /* OS detection */
        OSDetect osd = new OSDetect(projectPath);
        osd.whisperDetection();
        
        Path resource_path = Paths.get(osd.getResource_address());
//        if (!Files.exists(resource_path)) {
//            logger.error("resource 폴더가 존재하지 않습니다. resource 폴더를 다운받아 주세요.");
//            logger.error("resource 폴더를 둘 곳: "+ osd.getResource_address());
//            
//            response.put("Install the 'resource' folder at the following address: ", osd.getResource_address());
//            ObjectMapper objectMapper = new ObjectMapper();
//            String jsonResponse = objectMapper.writeValueAsString(response);
//            return new ResponseEntity<>(jsonResponse, headers, HttpStatus.OK);
//        }
        
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
			ModelAndView modelAndView = new ModelAndView();
			modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
			modelAndView.addObject("errorMessage", "오디오만 추출했음에도 파일의 크기가 26214400bytes를 초과합니다. 파일을 분할하여 주세요.");
			return modelAndView;
		}
        
        
        PythonModifier pc = new PythonModifier(osd, locOfPython, lang, absolutePathString);
        String whisperCommand = pc.getWhisperCommand();
        logger.debug("whisperCommand: " + whisperCommand);
        
        WhisperController wc = new WhisperController();
        wc.startWhisperProcess(osd, pc);
        
        String srt_content = osd.makeSrt(osd, fc);
        //file_size = extractedAudio.length();
        
        String summary_result = wc.getSrtResult(srt_content, searchfor);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        logger.info("Execution time:"+executionTime);
        logger.debug(summary_result);
        
        Result rslt = new Result();
        response = rslt.getResult(response, fc.getFile_size(), summary_result, executionTime, srt_content);

        //ObjectMapper objectMapper = new ObjectMapper();
        //String jsonResponse = objectMapper.writeValueAsString(response);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("result"); // 반환할 JSP 파일 경로 설정
		modelAndView.addObject("arkeeperresult", response); // 결과 데이터를 모델에 추가
		
        fc.deleteFile(origin_absolutePathString);
        fc.deleteSrtFile(osd.getSrt_address());
        
        return modelAndView;
     }
	 
	 @PostMapping("/minutes-mnv.do")
		@ResponseBody
		public ModelAndView extractTimestampMnV(@RequestParam MultipartFile file,
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
				ModelAndView modelAndView = new ModelAndView();
			    modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
			    return modelAndView;
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


			ModelAndView modelAndView = new ModelAndView();
	        modelAndView.setViewName("result"); // 반환할 JSP 파일 경로 설정
	        modelAndView.addObject("arkeeperresult", response); // 결과 데이터를 모델에 추가

	        return modelAndView;

		}
	 @PostMapping("/minutes-summary-mnv.do")
		@ResponseBody
		public ModelAndView MinuteSummaryMnV(@RequestParam MultipartFile file,
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
				ModelAndView modelAndView = new ModelAndView();
			    modelAndView.setViewName("egovError.jsp"); // 에러를 보여줄 JSP 파일 경로 설정
			    return modelAndView;
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


			ModelAndView modelAndView = new ModelAndView();
	        modelAndView.setViewName("result"); // 반환할 JSP 파일 경로 설정
	        modelAndView.addObject("arkeeperresult", response); // 결과 데이터를 모델에 추가

	        return modelAndView;

		}

}
