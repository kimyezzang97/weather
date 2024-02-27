package yezzang.weather.service;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yezzang.weather.WeatherApplication;
import yezzang.weather.domain.DateWeather;
import yezzang.weather.domain.Diary;
import yezzang.weather.error.InvalidDate;
import yezzang.weather.repository.DateWeatherRepository;
import yezzang.weather.repository.DiaryRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apiKey;

    // org.slf4j
    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);
    private final DiaryRepository diaryRepository;
    private final DateWeatherRepository dateWeatherRepository;

    public DiaryService(DiaryRepository diaryRepository, DateWeatherRepository dateWeatherRepository){
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    @Transactional() // 작성 안 해도 : readOnly = false
    public void createDiary(LocalDate date, String text){
        logger.info("started to create diary");
        // 날씨 데이터 가져오기 (DB에 이미 저장된 데이터 가져오기)
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터 + 일기 값 우리 db에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
        logger.info("end to create diary");
    }

    private String getWeatherString(){
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apiKey;

        System.out.println(apiUrl);

        try{
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            BufferedReader br;
            if(responseCode == 200){
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            }
            String inputLine;
            StringBuilder response = new StringBuilder();
            while((inputLine = br.readLine()) != null){
                response.append(inputLine);
            }
            br.close();

            return response.toString();
        } catch (Exception e){
            return "failed to get response";
        }

    }

    private Map<String, Object> parseWeather(String jsonString){
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Map<String, Object> resultMap = new HashMap<>();

        JSONObject mainData = (JSONObject) jsonObject.get("main");
        resultMap.put("temp", mainData.get("temp"));
        JSONArray jsonArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) jsonArray.get(0);
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));

        return resultMap;

    }

    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date){
        logger.debug("readDiary");
//        if(date.isAfter(LocalDate.ofYearDay(3050, 1))){
//            throw new InvalidDate(); // 3050년이 넘으면 만들었던 커스텀 exception 호출
//        }
        return diaryRepository.findAllByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate){
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    @Transactional()
    public void updateDiary(LocalDate date, String text){
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
    }

    @Transactional()
    public void deleteDiary(LocalDate date){
        diaryRepository.deleteAllByDate(date);
    }

    //@Scheduled(cron = "0/5 * * * * *") // 5초간격 테스트
    @Scheduled(cron = "0 0 1 * * *") // 새벽 1시마다 가동
    @Transactional() // readOnly = false
    public void saveWeatherDate(){
        dateWeatherRepository.save(getWeatherFromApi());
    }

    private DateWeather getWeatherFromApi(){
        // open weather map에서 날씨 데이터 가져오기
        String weatherData = getWeatherString();

        // 받아온 날씨 Json 파싱하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));
        return dateWeather;
    }

    private DateWeather getDateWeather(LocalDate date){
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if(dateWeatherListFromDB.size() == 0){
            // 새로 API에서 날씨 정보를 가져온다.
            // 정책상 현재 날씨를 가져오자
            return getWeatherFromApi();
        } else {
            return dateWeatherListFromDB.get(0);
        }

    }
}
