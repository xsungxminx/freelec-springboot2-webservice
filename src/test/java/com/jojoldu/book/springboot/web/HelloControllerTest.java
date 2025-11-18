package com.jojoldu.book.springboot.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class) // 테스트를 진행할떼 junit에 내장된 실행자 외에 다른 실행자를 실행시킵니다.
@WebMvcTest(controllers = HelloController.class) // @WebMvcTest 여러 스프링 테스트 어노테이션 중, Web(spring MVC)에 집중할 수 있는 어노테이션 입니다.

public class HelloControllerTest {

    @Autowired // 스프링이 관리하는 빈(bean)을 주입 받습니다.
    private MockMvc mvc; // 웹다. api를 테스트할때 사용합니  스프링 mvc 테스트의 시작점. 이 클래스를 통해 HTTP GET, POST 등에 대한 API 테스트를 할 수 있습니다.

    @WithMockUser(roles="USER")
    @Test
    public void hello가_리턴된다() throws Exception {
        String hello = "hello";

        mvc.perform(get("/hello"))      //MockMvc를 통해 /hello 주소로 http GET 요청을 합니다.  체이닝이 지원되어 아래와 같이 여러 검증 기능을 이어서 선언할 수 있습니다.
                .andExpect(status().isOk())       // mvc.perfom의 결과를 검증합니다. , http Header의 status를 검증합니다. , 우리가 흔히 알고 있는 200,404,500 등의 상태를 검증 , 여기선 즉 200인지 아닌지 검증
                .andExpect(content().string(hello)); // mvc.perform의 결과를 검증합니다. , 응답 본문의 내용을 검증. , controller에서 "hello"를 리턴하기 때문에 이 값이 맞는지 검증
    }

    @WithMockUser(roles="USER")
    @Test
    public void helloDto가_리턴된다() throws Exception {
        String name = "hello";
        int amount = 1000;

        mvc.perform(
                        get("/hello/dto")
                                .param("name", name) // param : API테스트 할때 사용될 요청 파라미터를 설정 , 값은 STring 만 허용, 그래서 숫자/날짜 등 데이터도 등록할때는 문자열로 변경해야 가능
                                .param("amount", String.valueOf(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(name))) // jsonPath : json응답값을 필드별로 검증할 수 있는 메소드, $를 기준으로 필드명을 명시합니다., 여기서는 #$name, $amount로 검증합니다.
                .andExpect(jsonPath("$.amount", is(amount)));
    }
}