package com.myseotoolbox.crawler.spider;

import com.myseotoolbox.crawler.httpclient.SnapshotException;
import com.myseotoolbox.crawler.httpclient.WebPageReader;
import com.myseotoolbox.crawler.model.CrawlResult;
import com.myseotoolbox.crawler.model.PageSnapshot;
import com.myseotoolbox.crawler.spider.model.SnapshotTask;
import com.myseotoolbox.crawler.testutils.CurrentThreadTestExecutorService;
import com.myseotoolbox.testUtils.LogEventMatcherBuilder;
import com.myseotoolbox.testUtils.TestAppender;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@RunWith(MockitoJUnitRunner.class)
public class CrawlersPoolTest {

    public static final PageSnapshot FAILURE_TEST_SNAPSHOT = new PageSnapshot();
    private static final CrawlResult TEST_SNAPSHOT_RESULT = CrawlResult.forSnapshot(new PageSnapshot());
    private static final URI SUCCESS_TEST_LINK = URI.create("http://host1");
    private static final URI FAILURE_TEST_LINK = URI.create("http://verybadhost");
    private TestAppender testAppender;
    @Mock
    private WebPageReader reader;
    @Mock
    private Consumer<CrawlResult> listener;
    private ThreadPoolExecutor executor = new CurrentThreadTestExecutorService();
    private CrawlersPool sut;

    @Before
    public void setUp() throws SnapshotException {
        when(reader.snapshotPage(SUCCESS_TEST_LINK)).thenReturn(TEST_SNAPSHOT_RESULT);
        when(reader.snapshotPage(FAILURE_TEST_LINK)).thenThrow(new SnapshotException(new RuntimeException("This one's not good"), FAILURE_TEST_SNAPSHOT));

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        testAppender = config.getAppender("TestAppender");

        Map<String, Appender> appenders = config.getAppenders();


        sut = new CrawlersPool(reader, executor);
    }

    @Test
    public void shouldSubmitSnapshotWhenSuccessful() {
        acceptTaskFor(SUCCESS_TEST_LINK);
        verify(listener).accept(TEST_SNAPSHOT_RESULT);
    }

    @Test
    public void shouldSubmitPartialValueWhenExceptionOccur() {
        acceptTaskFor(FAILURE_TEST_LINK);
        verify(listener).accept(argThat(argument -> argument.getPageSnapshot() == FAILURE_TEST_SNAPSHOT));
    }

    @Test
    public void exceptionHappeningOutsideCrawlShouldBeHandled() {
        doThrow(new RuntimeException("This happened while submitting result")).when(listener).accept(any());
        acceptTaskFor(SUCCESS_TEST_LINK);
        assertThat(testAppender.getMessages(), Matchers.hasItem(LogEventMatcherBuilder.logEvent().withLevel(Level.ERROR).withMessageContaining("Exception while crawling").build()));
    }


    private void acceptTaskFor(URI uri) {
        sut.accept(new SnapshotTask(uri, listener));
    }
}
