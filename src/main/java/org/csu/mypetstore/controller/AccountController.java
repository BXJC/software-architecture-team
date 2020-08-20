package org.csu.mypetstore.controller;

import org.csu.mypetstore.other.makeCode;
import org.springframework.util.DigestUtils;
import com.aliyuncs.exceptions.ClientException;
import org.csu.mypetstore.domain.Account;
import org.csu.mypetstore.domain.Cart;
import org.csu.mypetstore.service.AccountService;
import org.csu.mypetstore.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//注解，说明这是一个Controller类
@Controller
//接收所有的/accoount下的url的请求
@RequestMapping("/account")
//将account、cart、sms与session中的同名属性绑定
@SessionAttributes({"account","cart","sms"})



public class AccountController {
  //注解；将项目中的AccountService对象自动注入accountService中
  @Autowired
  AccountService accountService;
  //注解，同上
  @Autowired
  CartService cartService;

  private static final List<String> languageList;
  private static final List<String> categoryList;

  static {
    List<String> langList = new ArrayList<String> ();
    langList.add ("ENGLISH");
    langList.add ("CHINESE");
    languageList = Collections.unmodifiableList (langList);

    List<String> catList = new ArrayList<String> ();
    catList.add ("FISH");
    catList.add ("DOGS");
    catList.add ("REPTILES");
    catList.add ("CATS");
    catList.add ("BIRDS");

    categoryList = Collections.unmodifiableList (catList);
  }

  @GetMapping("/signOnForm")
  public String signOnForm(String method, Model model) {
    if (method.equals ("username")) {
      return "account/signOn.html";
    } else {
      model.addAttribute ("sms", "notexit");
      return "account/signOnByPhone.html";
    }
  }

  @PostMapping("/signOn")
  public String login(@SessionAttribute("strEnsure")String  strEnsure, String password, String username,String vcode, Model model) {
    Account account = accountService.getAccount (username, DigestUtils.md5DigestAsHex (password.getBytes ()));


    Cart cart = new Cart ();
    if(vcode !=strEnsure){
      model.addAttribute ("msg", "用户名或密码错误");
      return "account/signOn";
    }
      else if (account != null) {
      model.addAttribute ("account", account);
      cart = cartService.getCart (username);
      model.addAttribute ("cart", cart);
      return "catalog/main";
    } else {
      model.addAttribute ("msg", "用户名或密码错误");
      return "account/signOn";
    }
  }

  @PostMapping("/signOnByPhone")
  public String loginByPhone(String phoneNumber, @SessionAttribute String sms, String iCode, Model model) {
    Account account = accountService.getAccountByPhoneNumber (phoneNumber);
    Cart cart = new Cart ();
    if (account == null) {
      model.addAttribute ("msg", "用户不存在");
      return "account/signOn";
    } else if (!sms.equals (iCode)) {
      System.out.println ("sms:" + sms);
      System.out.println ("icode:" + iCode);
      model.addAttribute ("msg", "验证码错误");
      return "account/signOn";
    } else {
      model.addAttribute ("account", account);
      cart = cartService.getCart (account.getUsername ());
      model.addAttribute ("cart", cart);
      return "catalog/main";
    }
  }

  @GetMapping("/signOut")
  public String signOff(Model model, SessionStatus sessionStatus) {
    model.addAttribute ("account", null);
    model.addAttribute ("cart", null);
    sessionStatus.setComplete ();

    return "catalog/main";
  }

  @GetMapping("/editUserInfoForm")
  public String editUserInfoForm(@SessionAttribute("account") Account account, Model model) {
    model.addAttribute ("account", account);
    model.addAttribute ("languageList", languageList);
    model.addAttribute ("categoryList", categoryList);
    System.out.println (account);
    return "account/editAccount";
  }

  @PostMapping("/editUserInfo")
  public String editUserInfo(Account account, String repeatPassword, Model model) {
    String msg = null;

    if (account.getPassword ().equals (repeatPassword)) {
      account.setPassword (DigestUtils.md5DigestAsHex (account.getPassword ().getBytes ()));
      accountService.updateAccount (account);
      msg = "修改成功";
      model.addAttribute ("account", account);
      model.addAttribute ("msg", msg);
      return "account/editAccount";

    } else {
      model.addAttribute ("account", account);
      System.out.println (account.getPassword ());
      System.out.println (repeatPassword);
      msg = "两次输入密码不一致";
      model.addAttribute ("msg", msg);
      return "account/editAccount";
    }

  }

  @GetMapping("/newAccountForm")
  public String newAccountForm(String msg) {

    return "account/newAccount";
  }

  @PostMapping("/newAccount")
  public String newAccount(@SessionAttribute String sms, String username, String password, String repeatPassword, String inputCode, String phoneNumber, Model model) {
    Account account = new Account ();
    String msg = null;

    if (!sms.equals (inputCode)) {
      msg = "验证码不正确";
      model.addAttribute ("msg", msg);
      return "account/newAccount";
    } else if (accountService.checkPhone (phoneNumber)) {
      msg = "手机号已被注册";
      model.addAttribute ("msg", msg);
      return "account/newAccount";
    } else if (accountService.getAccount (username) != null) {
      msg = "用户名已被使用";
      model.addAttribute ("msg", msg);
      return "account/newAccount";
    } else if (password.equals (repeatPassword)) {
      account.setUsername (username);
      account.setPassword (DigestUtils.md5DigestAsHex (password.getBytes ()));
      account.setPhone (phoneNumber);

      accountService.insertAccount (account);
      return "account/signOn";

    } else {
      msg = "两次密码输入不一致";
      model.addAttribute ("msg", msg);
      return "account/newAccount";
    }

  }

  @GetMapping("/sendVCode")
  @ResponseBody
  public String sendVCode(String phoneNumber, Model model, HttpSession session) throws ClientException {
    System.out.println ("手机号" + phoneNumber);
    String sms = accountService.sendMsg (phoneNumber);
    String msg;
    System.out.println ("smss" + sms);

    if (sms != null) {
      msg = "验证码发送成功";
      model.addAttribute ("sms", sms);
      session.setAttribute ("sms", sms);
    } else {
      msg = "验证码发送失败";
    }
    return msg;

  }


  @GetMapping("/check")
  public void createImg(HttpServletRequest request, HttpServletResponse response) throws IOException {

    //禁用缓存，每次访问此页面，都重新生成
    response.setHeader("Pragma","No-cache");
    response.setHeader("Cache-Control","no-cache");
    response.setDateHeader("Expires", 0);

    //生成验证码的实例对象
    makeCode ie = new makeCode();

    //调用里面的方法，返回的是生成的验证码中的字符串
    String code = ie.getEnsure(0,0,response.getOutputStream());
    System.out.println("the code is "+code);


    //获得session，并把字符串保存在session中，为后面的对比做基础
    HttpSession session = request.getSession();
    session.setAttribute("strEnsure", code);
    System.out.println ("验证码"+session.getAttribute ("strEnsure"));

  }
}
