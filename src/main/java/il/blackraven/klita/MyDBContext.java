package il.blackraven.klita;

import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.db.Var;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyDBContext implements DBContext {
    @Override
    public <T> List<T> getList(String s) {
        return null;
    }

    @Override
    public <K, V> Map<K, V> getMap(String s) {
        return null;
    }

    @Override
    public <T> Set<T> getSet(String s) {
        return null;
    }

    @Override
    public <T> Var<T> getVar(String s) {
        return null;
    }

    @Override
    public String summary() {
        return null;
    }

    @Override
    public Object backup() {
        return null;
    }

    @Override
    public boolean recover(Object o) {
        return false;
    }

    @Override
    public String info(String s) {
        return null;
    }

    @Override
    public void commit() {

    }

    @Override
    public void clear() {

    }

    @Override
    public boolean contains(String s) {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
