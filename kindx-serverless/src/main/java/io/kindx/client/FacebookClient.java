package io.kindx.client;

import feign.Param;
import feign.RequestLine;
import io.kindx.dto.facebook.FacebookPageDto;
import io.kindx.dto.facebook.FacebookPostDto;

//TODO: Cache this client
public interface FacebookClient {
    String PAGE_FIELDS = "id,phone,location,single_line_address,name,name_with_location_descriptor," +
            "is_always_open,hours,emails,about,description,cover,username,picture,website";

    String POST_FIELDS = "id,place,message,created_time,story";


    @RequestLine("GET /{username}?access_token={token}&fields="
            + PAGE_FIELDS)
    FacebookPageDto getFacebookPage(@Param("token") String accessToken,
                                    @Param("username") String pageUsername);

    @RequestLine("GET /{username}?access_token={token}&fields="
            + PAGE_FIELDS
            + ",posts.since({sinceDate}){" + POST_FIELDS + "}")
    FacebookPageDto getFacebookPageWithPosts(@Param("token") String accessToken,
                                             @Param("username") String pageUsername,
                                             @Param("sinceDate") String sinceDate);



    @RequestLine("GET /{username}/tagged?access_token={token}&since={sinceDate}&after={afterToken}&fields=" + POST_FIELDS)
    FacebookPageDto.PagePosts getTaggedPosts(@Param("token") String accessToken,
                                             @Param("username") String pageUsername,
                                             @Param("sinceDate") String sinceDate,
                                             @Param("afterToken") String afterToken);


    @RequestLine("GET /{post_id}?access_token={token}&fields=" + POST_FIELDS)
    FacebookPostDto getPageSinglePost(@Param("token") String accessToken,
                                              @Param("post_id") String postId);

}
