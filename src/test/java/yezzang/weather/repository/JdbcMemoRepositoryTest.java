package yezzang.weather.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import yezzang.weather.domain.Memo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 이 어노테이션이 있으면 테스트 환경에서 아무런 작업을 해도 원상태로 복구 된다.
class JdbcMemoRepositoryTest {

    @Autowired
    JdbcMemoRepository jdbcMemoRepository;

    @Test
    void insertMemoTest(){
        //given
        Memo newMemo = new Memo(1, "this is new memo~");

        //when
        jdbcMemoRepository.save(newMemo);

        //then (대부분 assert문이 들어감)
        Optional<Memo> result = jdbcMemoRepository.findById(1);
        assertEquals(result.get().getText(), "this is new memo~");
    }

    @Test
    void findAllMemoTest(){
        //given
        List<Memo> memoList = jdbcMemoRepository.findAll();
        System.out.println(memoList);

        //when

        //then
        assertNotNull(memoList);
    }
}