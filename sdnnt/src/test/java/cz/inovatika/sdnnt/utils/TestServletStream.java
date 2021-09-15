package cz.inovatika.sdnnt.utils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class TestServletStream extends ServletInputStream {

    public ByteArrayInputStream bis;

    public TestServletStream(ByteArrayInputStream bis) {
        this.bis = bis;
    }

    @Override
    public boolean isFinished() {
        try {
            return bis.available() <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isReady() {
        return bis.available() > 0;
    }

    @Override
    public void setReadListener(ReadListener readListener) {

    }

    @Override
    public int read() throws IOException {
        return bis.read();
    }

}
