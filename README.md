# kino

A Clojure library designed to ... well, that part is up to you.

## Usage

### DB

```sh
docker run --name kino-postgres -e POSTGRES_PASSWORD=kino -p 5432:5432 -d postgres
```

```
docker exec -it kino-postgres sh
```

```
psql -U postgres
```

```sh
create database kino;
```

```
create user kino with password 'kino';
```

```
grant all privileges on database kino to kino;
```

```
alter database kino owner to kino;
```

FIXME

## License

The MIT License (MIT)

Copyright © 2019 Kostas Georgiadis

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
