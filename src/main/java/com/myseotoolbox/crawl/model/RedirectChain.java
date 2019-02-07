package com.myseotoolbox.crawl.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.myseotoolbox.crawl.httpclient.WebPageReader.isRedirect;


@Getter
@Setter
@ToString
public final class RedirectChain {

    private final List<RedirectChainElement> elements;
    private InputStream inputStream;


    public RedirectChain() {
        elements = new ArrayList<>();
    }

    public void addElement(RedirectChainElement redirectChainElement) throws RedirectLoopException {
        elements.add(redirectChainElement);

        if (isRedirectLoop(redirectChainElement)) {
            throw new RedirectLoopException();
        }
    }

    public List<RedirectChainElement> getElements() {
        return new ArrayList<>(elements);
    }

    public String getDestinationURI() {
        return getLastElement().getDestinationURI();
    }

    private boolean isRedirectLoop(RedirectChainElement redirectChainElement) {
        return isRedirect(redirectChainElement.getHttpStatus()) && alreadyExistInTheChain(redirectChainElement);
    }

    private boolean alreadyExistInTheChain(RedirectChainElement redirectChainElement) {
        return elements.stream()
                .map(RedirectChainElement::getSourceURI)
                .anyMatch(uri -> uri.equals(redirectChainElement.getDestinationURI()));
    }

    private RedirectChainElement getLastElement() {
        return elements.get(elements.size() - 1);
    }

}


