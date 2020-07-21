import org.junit.Test

class FileUtilsTest {

    private final fileUtils = new fileUtils()

    @Test
    void returnFileNameWithoutExtension() {
        [
         ["c:/somedir\\filename1.txt", "filename1"],
         ["c:\\somedir\\file.name2.txt", "file.name2"],
         ['filename3.txt', 'filename3'],
         ['file.name4.txt', 'file.name4'],
         ['/somedir/filename5.txt', 'filename5'],
         ['/some.dir/filename6.txt', 'filename6'],
         ['c:\\some.dir\\file.name7.txt', 'file.name7'],
         ['c:\\some.dir\\filename8.txt', 'filename8'],
         ['c:\\some.dir\\filename9', 'filename9'],
         ['c:\\some.dir\\filename10.txt', 'filename10'],
         ['c:\\some.dir\\somesubdir11.xt', 'somesubdir11'],
         ['some.dir\\somesubdir12.xt', 'somesubdir12'],
         ['somedir\\somesubdir13', 'somesubdir13'],
         ['somedir/somesubdir14', 'somesubdir14'],
         ['somedir/somesubdir15.txt', 'somesubdir15']
        ].each { path, expectedName -> checkFileName(path, expectedName)}
    }

    private checkFileName(String fullFileName, expectedName) {
        assert fileUtils.getFileNameWithoutExtension(fullFileName) == expectedName
    }
}
