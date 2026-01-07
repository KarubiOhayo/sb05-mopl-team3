package io.mopl.api.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

  @GetMapping({
    "/profiles/**",
    "/playlists/**",
    "/contents/**",
    "/conversations/**",
    "/notifications/**"
  })
  public String forward() {
    return "forward:/index.html";
  }
}
