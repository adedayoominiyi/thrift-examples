service HelloSvc {
    string hello_func()
}

typedef string FirstName
typedef string LastName
struct FullName {
    1: string FirstName
    2: string LastName
}

enum Gender {
    MALE = 1
    FEMALE = 2
}

union Age {
    1: i8 age
    2: i16 year 
}

struct HelloResponse {
    1: string helloMessage
}

service HelloSvc2 {
    HelloResponse hello_func(1: FullName full_name, 2: Gender gender)
}