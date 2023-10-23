package org.qbicc.interpreter.impl;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmReferenceArray;
import org.qbicc.machine.arch.Platform;

/**
 *
 */
final class HooksForSystemPropsRaw {
    HooksForSystemPropsRaw() {}

    @Hook
    static VmReferenceArray platformProperties(VmThreadImpl thread) {
        final VmImpl vm = thread.vm;
        final CompilationContext ctxt = vm.getCompilationContext();

        // todo: configuration
        Locale displayLocale = Locale.getDefault();
        Locale formatLocale = Locale.getDefault();
        Charset fileEncoding = StandardCharsets.UTF_8;
        Platform platform = ctxt.getPlatform();
        String tempDir = "/tmp";
        Charset jnuEncoding = StandardCharsets.UTF_8;
        Charset stderrEncoding = StandardCharsets.UTF_8;
        Charset stdoutEncoding = StandardCharsets.UTF_8;

        return fromStringList(vm, List.of(
            //        @Native private static final int _display_country_NDX = 0;
            displayLocale.getCountry(),
            //        @Native private static final int _display_language_NDX = 1 + _display_country_NDX;
            displayLocale.getLanguage(),
            //        @Native private static final int _display_script_NDX = 1 + _display_language_NDX;
            displayLocale.getScript(),
            //        @Native private static final int _display_variant_NDX = 1 + _display_script_NDX;
            displayLocale.getVariant(),
            //        @Native private static final int _file_encoding_NDX = 1 + _display_variant_NDX;
            fileEncoding.name(),
            //        @Native private static final int _file_separator_NDX = 1 + _file_encoding_NDX;
            platform.os().fileSeparator(),
            //        @Native private static final int _format_country_NDX = 1 + _file_separator_NDX;
            formatLocale.getCountry(),
            //        @Native private static final int _format_language_NDX = 1 + _format_country_NDX;
            formatLocale.getLanguage(),
            //        @Native private static final int _format_script_NDX = 1 + _format_language_NDX;
            formatLocale.getScript(),
            //        @Native private static final int _format_variant_NDX = 1 + _format_script_NDX;
            formatLocale.getVariant(),
            //        @Native private static final int _ftp_nonProxyHosts_NDX = 1 + _format_variant_NDX;
            "",
            //        @Native private static final int _ftp_proxyHost_NDX = 1 + _ftp_nonProxyHosts_NDX;
            "",
            //        @Native private static final int _ftp_proxyPort_NDX = 1 + _ftp_proxyHost_NDX;
            "",
            //        @Native private static final int _http_nonProxyHosts_NDX = 1 + _ftp_proxyPort_NDX;
            "",
            //        @Native private static final int _http_proxyHost_NDX = 1 + _http_nonProxyHosts_NDX;
            "",
            //        @Native private static final int _http_proxyPort_NDX = 1 + _http_proxyHost_NDX;
            "",
            //        @Native private static final int _https_proxyHost_NDX = 1 + _http_proxyPort_NDX;
            "",
            //        @Native private static final int _https_proxyPort_NDX = 1 + _https_proxyHost_NDX;
            "",
            //        @Native private static final int _java_io_tmpdir_NDX = 1 + _https_proxyPort_NDX;
            tempDir,
            //        @Native private static final int _line_separator_NDX = 1 + _java_io_tmpdir_NDX;
            platform.os().lineSeparator(),
            //        @Native private static final int _os_arch_NDX = 1 + _line_separator_NDX;
            platform.cpu().name(),
            //        @Native private static final int _os_name_NDX = 1 + _os_arch_NDX;
            platform.os().name(),
            //        @Native private static final int _os_version_NDX = 1 + _os_name_NDX;
            "generic version",
            //        @Native private static final int _path_separator_NDX = 1 + _os_version_NDX;
            platform.os().pathSeparator(),
            //        @Native private static final int _socksNonProxyHosts_NDX = 1 + _path_separator_NDX;
            "",
            //        @Native private static final int _socksProxyHost_NDX = 1 + _socksNonProxyHosts_NDX;
            "",
            //        @Native private static final int _socksProxyPort_NDX = 1 + _socksProxyHost_NDX;
            "",
            //        @Native private static final int _sun_arch_abi_NDX = 1 + _socksProxyPort_NDX;
            platform.abi().name(),
            //        @Native private static final int _sun_arch_data_model_NDX = 1 + _sun_arch_abi_NDX;
            String.valueOf(platform.cpu().wordSize() << 3),
            //        @Native private static final int _sun_cpu_endian_NDX = 1 + _sun_arch_data_model_NDX;
            platform.cpu().byteOrder() == ByteOrder.BIG_ENDIAN ? "big" : "little",
            //        @Native private static final int _sun_cpu_isalist_NDX = 1 + _sun_cpu_endian_NDX;
            "",
            //        @Native private static final int _sun_io_unicode_encoding_NDX = 1 + _sun_cpu_isalist_NDX;
            platform.cpu().byteOrder() == ByteOrder.BIG_ENDIAN ? "UnicodeBig" : "UnicodeLittle",
            //        @Native private static final int _sun_jnu_encoding_NDX = 1 + _sun_io_unicode_encoding_NDX;
            jnuEncoding.name(),
            //        @Native private static final int _sun_os_patch_level_NDX = 1 + _sun_jnu_encoding_NDX;
            "",
            //        @Native private static final int _sun_stderr_encoding_NDX = 1 + _sun_os_patch_level_NDX;
            stderrEncoding.name(),
            //        @Native private static final int _sun_stdout_encoding_NDX = 1 + _sun_stderr_encoding_NDX;
            stdoutEncoding.name(),
            //        @Native private static final int _user_dir_NDX = 1 + _sun_stdout_encoding_NDX;
            "/qbicc/build",
            //        @Native private static final int _user_home_NDX = 1 + _user_dir_NDX;
            "/qbicc/build/home",
            //        @Native private static final int _user_name_NDX = 1 + _user_home_NDX;
            "nobody")
            //        @Native private static final int FIXED_LENGTH = 1 + _user_name_NDX;
        );
    }

    @Hook
    static VmArray vmProperties(final VmThreadImpl thread) {
        List<String> props =  List.of(
            "java.home",    "/qbicc/java.home",
            "user.timezone", ZoneId.systemDefault().getId()
        );

        // Now add in the properties from the qbicc command line
        if (!thread.vm.getPropertyDefines().isEmpty()) {
            ArrayList<String> combined = new ArrayList<>(props);
            combined.addAll(thread.vm.getPropertyDefines());
            props = combined;
        }

        return fromStringList(thread.vm, props);
    }

    private static VmReferenceArray fromStringList(VmImpl vm, List<String> list) {
        int size = list.size();
        VmReferenceArray array = vm.newArrayOf(vm.stringClass, size);
        VmObject[] arrayArray = array.getArray();
        for (int i = 0; i < size; i ++) {
            arrayArray[i] = vm.intern(list.get(i));
        }
        return array;
    }

}
