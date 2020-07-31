FROM python:3-onbuild
COPY . /user/src/app/
WORKDIR /user/src/app
#Install python libraries
RUN set -e; pip install -r requirements.txt
CMD ["python","dummyfrontend.py"]