package com.myseotoolbox.crawler.utils;

import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;


@Log4j2
public class FunctionalExceptionUtils {

    public static void runOrLogWarning(CheckedRunnable task, String msg) {
        Try.run(task).orElseRun(throwable -> log.warn(msg, throwable));
    }

}
