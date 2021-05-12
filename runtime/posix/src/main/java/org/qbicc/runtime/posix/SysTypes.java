package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;

import org.qbicc.runtime.Build;

/**
 *
 */
@include(value = "<sys/types.h>", when = Build.Target.IsPosix.class)
public final class SysTypes {

    public static final String LARGEFILE64_SOURCE = "_LARGEFILE64_SOURCE";

    public static final class ssize_t extends word {
    }

    public static final class pid_t extends word {
    }

    public static final class off_t extends word {
    }

    public static final class off_t_ptr extends ptr<off_t> {}
    public static final class const_off_t_ptr extends ptr<@c_const off_t> {}
    public static final class off_t_ptr_ptr extends ptr<off_t_ptr> {}
    public static final class const_off_t_ptr_ptr extends ptr<const_off_t_ptr> {}
    public static final class off_t_ptr_const_ptr extends ptr<@c_const off_t_ptr> {}
    public static final class const_off_t_ptr_const_ptr extends ptr<@c_const const_off_t_ptr> {}


    @define(LARGEFILE64_SOURCE)
    public static final class off64_t extends word {
    }

    public static final class loff_t extends word {
    }

    public static final class loff_t_ptr extends ptr<loff_t> {}
    public static final class const_loff_t_ptr extends ptr<@c_const loff_t> {}
    public static final class loff_t_ptr_ptr extends ptr<loff_t_ptr> {}
    public static final class const_loff_t_ptr_ptr extends ptr<const_loff_t_ptr> {}
    public static final class loff_t_ptr_const_ptr extends ptr<@c_const loff_t_ptr> {}
    public static final class const_loff_t_ptr_const_ptr extends ptr<@c_const const_loff_t_ptr> {}

    public static final class uid_t extends word {
    }

    public static final class gid_t extends word {
    }

    @name("int") // integer type
    public static final class mode_t extends word {
    }

    public static final class dev_t extends word {
    }

    public static final class blkcnt_t extends word {
    }

    public static final class blksize_t extends word {
    }

    public static final class ino_t extends word {
    }

    public static final class nlink_t extends word {
    }

    public static final class time_t extends word {
    }


}
