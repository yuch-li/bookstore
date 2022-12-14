package com.web;

import com.entity.User;
import com.google.gson.Gson;
import com.service.UserService;
import com.service.impl.UserServiceImpl;
import com.utils.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY;


public class UserServlet extends BaseServlet {

    private UserService userService = new UserServiceImpl();

    public void regist(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String username = req.getParameter("username");
        User user = WebUtils.copyParamToBean(req.getParameterMap(), new User());
        String code = req.getParameter("code");

        HttpSession session = req.getSession();
        String kaptchaCode = (String) session.getAttribute(KAPTCHA_SESSION_KEY);

        //对比验证码是否正确，如果错误回显注册信息，正确则删除服务器中的验证码，防止表单重复提交
        if (!code.equalsIgnoreCase(kaptchaCode)) {
            req.setAttribute("username", username);
            req.setAttribute("password", req.getParameter("password"));
            req.setAttribute("repwd", req.getParameter("repwd"));
            req.setAttribute("email", req.getParameter("email"));
            req.setAttribute("errorMge", "验证码错误！");
            req.getRequestDispatcher("/pages/user/regist.jsp").forward(req,resp);
            return;
        } else session.removeAttribute(KAPTCHA_SESSION_KEY);

        userService.registUser(user);
        session.setAttribute("username", username);
        try {
            req.getRequestDispatcher("/pages/user/regist_success.jsp").forward(req, resp);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void login(HttpServletRequest req, HttpServletResponse resp) {

        String username = req.getParameter("username");
        String password = req.getParameter("password");
        User user = WebUtils.copyParamToBean(req.getParameterMap(), new User());

        if (userService.loginUser(user) == null) {
            req.setAttribute("errorMge", "用户名或密码错误");
            req.setAttribute("username", username);
            try {
                req.getRequestDispatcher("/pages/user/login.jsp").forward(req, resp);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        //用户名密码正确
        } else {
            //创建Cookie，设置存活时间为一周
            Cookie usernameCookie = new Cookie("username", username);
            Cookie passwordCookie = new Cookie("password", password);
            usernameCookie.setMaxAge(60 * 60 * 24 * 7);
            passwordCookie.setMaxAge(60 * 60 * 24 * 7);
            resp.addCookie(usernameCookie);
            resp.addCookie(passwordCookie);

            //设置session，时长为30分钟，这里的时长是指浏览器不向服务器发送请求的时长
            HttpSession session = req.getSession();
            req.getSession().setMaxInactiveInterval(30);
            req.getSession().setAttribute("username", username);
            req.getSession().setAttribute("userid", userService.loginUser(user).getUserid());

            try {
                req.getRequestDispatcher("/pages/user/login_success.jsp").forward(req, resp);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logout(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().invalidate();
        resp.sendRedirect(req.getContextPath() + "/pages/user/login.jsp");
    }

    /**
     * 用Gson将查询结果封装成json字符串发送
     * @param req
     * @param resp
     */
     public void existUsername(HttpServletRequest req, HttpServletResponse resp) {
         String username = req.getParameter("username");
         boolean result = userService.existUsername(username);
         Map<String, Boolean> resultMap = new HashMap<>();
         resultMap.put("result", result);
         Gson gson = new Gson();
         try {
             resp.getWriter().write(gson.toJson(resultMap));
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
}
