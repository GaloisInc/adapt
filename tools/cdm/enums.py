from enum import Enum


class Instrumentationsource(Enum):
    LINUX_AUDIT_TRACE = 0
    LINUX_PROC_TRACE = 1
    LINUX_BEEP_TRACE = 2
    FREEBSD_OPENBSM_TRACE = 3
    ANDROID_JAVA_CLEARSCOPE = 4
    ANDROID_NATIVE_CLEARSCOPE = 5
    LINUX_AUDIT_CADETS = 6
    WINDOWS_DIFT_FAROS = 7


class Principal(Enum):
    LOCAL = 0
    REMOTE = 1


class Event(Enum):
    ACCEPT = 0
    BIND = 1
    CHANGE_PRINCIPAL = 2
    CHECK_FILE_ATTRIBUTES = 3
    CLONE = 4
    CLOSE = 5
    CONNECT = 6
    CREATE_OBJECT = 7
    CREATE_THREAD = 8
    EXECUTE = 9
    FORK = 10
    LINK = 11
    UNLINK = 12
    MMAP = 13
    MODIFY_FILE_ATTRIBUTES = 14
    MPROTECT = 15
    OPEN = 16
    READ = 17
    RENAME = 18
    WRITE = 19
    SIGNAL = 20
    TRUNCATE = 21
    WAIT = 22
    BLIND = 23
    UNIT = 24
    UPDATE = 25


class Source(Enum):
    ACCELEROMETER = 0
    TEMPERATURE = 1
    GYROSCOPE = 2
    MAGNETIC_FIELD = 3
    HEART_RATE = 4
    LIGHT = 5
    PROXIMITY = 6
    PRESSURE = 7
    RELATIVE_HUMIDITY = 8
    LINEAR_ACCELERATION = 9
    MOTION = 10
    STEP_DETECTOR = 11
    STEP_COUNTER = 12
    TILT_DETECTOR = 13
    ROTATION_VECTOR = 14
    GRAVITY = 15
    GEOMAGNETIC_ROTATION_VECTOR = 16
    CAMERA = 17
    GPS = 18


class Integritytag(Enum):
    UNTRUSTED = 0
    BENIGN = 1
    INVULNERABLE = 2


class Confidentialitytag(Enum):
    SECRET = 0
    SENSITIVE = 1
    PRIVATE = 2
    PUBLIC = 3


class Subject(Enum):
    PROCESS = 0
    THREAD = 1
    UNIT = 2
    BLOCK = 3
    EVENT = 4


class Strength(Enum):
    WEAK = 0
    MEDIUM = 1
    STRONG = 2


class Derivation(Enum):
    COPY = 0
    ENCODE = 1
    COMPILE = 2
    ENCRYPT = 3
    OTHER = 4
