/*
 * Copyright (c) 2019. Aaron Beetstra & Anti-Social Engineers
 *
 *  All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT license.
 *
 */

package acecardapi.utils;

import io.vertx.ext.mail.MailMessage;

public class EmailMessages {

  private static String mailCloser() {
    return "<br/>" +
      "<br/>" +
      "Met vriendelijke groet," +
      "<br/>" +
      "Ace of Clubs";
  }

  public static MailMessage registrationMail(String destinationMail, String registrationKey, String mailActivationLink) {

    String html = String.format("" +
      "Beste klant," +
      "<br/>" +
      "<br/>" +
      "Bedankt voor het registreren van uw account." +
      "<br/>" +
      "U moet uw account nog activeren, dit kunt u doen door op de onderstaande link te klikken:" +
      "<br/>" +
      "%s", mailActivationLink + registrationKey)
      + mailCloser();

    MailMessage message = new MailMessage();
    message.setFrom("noreply@aceofclubs.nl");
    message.setTo(destinationMail);
    message.setSubject("Email verificatie - Ace of Clubs");
    message.setHtml(html);

    return message;
  }

  public static MailMessage passwordResetMail(String destinationMail, String resetToken, String passwordResetLink) {

    String html = String.format("" +
      "Beste klant, <br/><br/>" +
      "U heeft bij ons aangegeven dat u uw wachtwoord bent vergeten. <br/>" +
      "U kunt een nieuw wachtwoord instellen via de onderstaande link: <br/>" +
      "%s", passwordResetLink + resetToken) +
      "<br/>" +
      "Bent u dit niet geweest? Dan kunt u deze mail negeren."
      + mailCloser();

    MailMessage message = new MailMessage();
    message.setFrom("noreply@aceofclubs.nl");
    message.setTo(destinationMail);
    message.setSubject("Wachtwoord reset - Ace of Clubs");
    message.setHtml(html);

    return message;
  }

}
