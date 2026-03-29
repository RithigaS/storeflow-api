package com.grootan.storeflow.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>StoreFlow API</title>
                    <style>
                        body {
                            margin: 0;
                            font-family: Arial, sans-serif;
                            background: linear-gradient(135deg, #0f172a, #1e293b);
                            color: white;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                        }
                        .container {
                            text-align: center;
                            background: rgba(255, 255, 255, 0.08);
                            padding: 40px;
                            border-radius: 16px;
                            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
                            max-width: 700px;
                            width: 90%;
                        }
                        h1 {
                            margin-bottom: 10px;
                            font-size: 36px;
                        }
                        p {
                            margin-bottom: 25px;
                            font-size: 18px;
                            color: #cbd5e1;
                        }
                        .btn {
                            display: inline-block;
                            padding: 14px 28px;
                            background: #38bdf8;
                            color: #0f172a;
                            text-decoration: none;
                            font-weight: bold;
                            border-radius: 10px;
                            transition: 0.3s ease;
                        }
                        .btn:hover {
                            background: #0ea5e9;
                            transform: translateY(-2px);
                        }
                        .links {
                            margin-top: 20px;
                        }
                        .links a {
                            color: #93c5fd;
                            text-decoration: none;
                            margin: 0 10px;
                        }
                        .links a:hover {
                            text-decoration: underline;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>StoreFlow API</h1>
                        <p>Production-ready backend for products, orders, authentication, files, reports, and notifications.</p>
                        <a class="btn" href="/swagger-ui/index.html">Open Swagger UI</a>
                        <div class="links">
                            <a href="/api/health">Health Check</a>
                            <a href="/actuator/health">Actuator Health</a>
                        </div>
                    </div>
                </body>
                </html>
                """;
    }
}
