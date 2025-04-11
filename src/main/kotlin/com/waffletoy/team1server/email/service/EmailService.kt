package com.waffletoy.team1server.email.service

import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import com.waffletoy.team1server.email.EmailSendFailureException
import com.waffletoy.team1server.email.EmailType
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    private val templateEngine: SpringTemplateEngine,
    @Value("\${custom.domain-name}") private val domainName: String,
) {
    @Async
    fun sendEmail(
        type: EmailType,
        to: String,
        subject: String,
        text: String,
        coffeeChatEntity: CoffeeChatEntity? = null,
    ) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            val context = Context()

            val templateName: String

            when (type) {
                EmailType.VerifyMail -> {
                    context.setVariable("emailCode", text)
                    context.setVariable("email", to)
                    templateName = "verifyEmail"
                }
                EmailType.ResetPassword -> {
                    context.setVariable("newPassword", text)
                    context.setVariable("email", to)
                    templateName = "resetPassword"
                }
                EmailType.Notification -> {
                    context.setVariable("domain", domainName)
                    context.setVariable("email", to)
                    if (coffeeChatEntity != null) {
                        context.setVariable("title", coffeeChatEntity.position.positionTitle)
                        context.setVariable("name", coffeeChatEntity.applicant.name)
                        context.setVariable("content", coffeeChatEntity.content)
                        context.setVariable("coffeeChatId", coffeeChatEntity.id)
                    }
                    templateName = "coffeeChatNotification"
                }
            }

            // Thymeleaf 템플릿
            val content: String = templateEngine.process(templateName, context)

            // 이메일 정보 설정
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(content, true)

            mailSender.send(message)
        } catch (ex: Exception) {
            throw EmailSendFailureException()
        }
    }
}
