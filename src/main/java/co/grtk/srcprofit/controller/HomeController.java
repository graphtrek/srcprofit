package co.grtk.srcprofit.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);
    private static final String INDEX_PAGE_PATH = "index";
    private static final String DASHBOARD_PAGE_PATH = "dashboard";

    @GetMapping("/")
    public String home(Model model) {
        return INDEX_PAGE_PATH;
    }

    @GetMapping("/index")
    public String index(Model model) {
        return INDEX_PAGE_PATH;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return DASHBOARD_PAGE_PATH;
    }
}
