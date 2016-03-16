package localizacion_actual.example.com.localizacion_actual;

import android.app.Activity;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.location.Location;
import android.widget.Toast;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.BaseImplementation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.api.d;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public class MainActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener {

    protected static final String TAG = "Actualizar_Ubicacion";

    // Intervalo de la actualizacion: 10 segundos
    public static final long TIEMPO_ACTUALIZACION = 5000;
    // La actualización se efectuará entre 5 y 10 segundos
    public static final long INTERVALO_ACTUALIZACION = TIEMPO_ACTUALIZACION / 2;
    protected final static String CLAVE_ACTUALIZACION = "CA";
    protected final static String CLAVE_UBICACION = "CU";
    protected final static String ULTIMA_ACTUALIZACION = "UA";

    protected GoogleApiClient gac;
    // Aquí guardaremos los parametros para solicitar la ubicación
    protected LocationRequest lr;
    // Creamos la ubicación geográfica como tal
    protected Location location;

    TextView longitud, latitud, tiempo;
    Button iniciar, parar;

    protected Boolean ubicacion;
    protected String tiempo_actualizacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iniciar = (Button) findViewById(R.id.iniciar);
        parar = (Button) findViewById(R.id.parar);
        latitud = (TextView) findViewById(R.id.latitudR);
        longitud = (TextView) findViewById(R.id.longitudR);
        tiempo = (TextView) findViewById(R.id.tiempo);
        ubicacion = false;
        tiempo_actualizacion = "";

        updateValuesFromBundle(savedInstanceState);
        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient(){
        Log.i(TAG, "Creando GoogleApiClient");
        gac = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    protected void createLocationRequest(){
        lr = new LocationRequest();
        // Asignamos el intervalo deseado para la actualización de la ubicación
        // Este intervalo no es siempre exacto. Puede ser más rápido o lento dependiendo de la velocidad de la app
        lr.setFastestInterval(INTERVALO_ACTUALIZACION);
        // Decimos que queremos que sea lo más precido posible
        lr.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // Es el que maneja el inicio de las actualizaciones y hace el request para las actualizaciones de la ubicación
    // Y no hace nada si ya fue solicitada la actualización
    public void startUpdatesButtonHandler(View view){
        if(!ubicacion){
            ubicacion = true;
            setButtonEnableState();
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates(){
        // Esto hace el request de la actualización del status
        LocationServices.FusedLocationApi.requestLocationUpdates(gac, lr, this);
    }

    // Con esto se actualiza la interfaz de usuario (los textos planos)
    private void updateUi(){
        latitud.setText("La latitud es: "+String.valueOf(location.getLatitude()));
        longitud.setText("La longitud es: "+String.valueOf(location.getLongitude()));
        tiempo.setText("Última actualización: " + tiempo_actualizacion);
    }

    public void stopUpdatesButtonHandler(View view){
        if(ubicacion){
            ubicacion = false;
            setButtonEnableState();
            stopLocationUpdates();
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Actualizando valores");

        if(savedInstanceState != null){
            // Si el savedInstanceState contiene la clave de actualización...
            if(savedInstanceState.keySet().contains(CLAVE_ACTUALIZACION)){
                // Le decimos que nos guarde el status de la actualización en esta variable
                ubicacion = savedInstanceState.getBoolean(CLAVE_ACTUALIZACION);
                setButtonEnableState();
            }
        }

        if(savedInstanceState.keySet().contains(CLAVE_UBICACION)){
            location = savedInstanceState.getParcelable(CLAVE_UBICACION);
        }

        if(savedInstanceState.keySet().contains(ULTIMA_ACTUALIZACION)){
            tiempo_actualizacion = savedInstanceState.getString(ULTIMA_ACTUALIZACION);
        }

        updateUi();
    }

    // Actualizamos la ubicación actual del Bundle y la mostramos a través de la UI
    private void setButtonEnableState(){
        if(ubicacion){
            iniciar.setEnabled(false);
            parar.setEnabled(true);
        }else{
            iniciar.setEnabled(true);
            parar.setEnabled(false);
        }
    }

    // Con esto se deja de solicitar actualizaciones de la ubicación
    protected void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(gac, this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        gac.connect(); // Le pedimos que se conecte
    }

    @Override
    protected void onResume(){
        super.onResume();
        // Si el objeto GoogleApiClient está conectado y además se tiene una ubicación...
        if(gac.isConnected() && ubicacion){
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        stopLocationUpdates();
    }

    @Override
    protected void onStop(){
        super.onStop();
        // Si está conectado le decimos que se desconecte
        if(gac.isConnected()){
            gac.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Conectado GoogleApiClient");

        // Si la ubicación inicial no fue solicitada entonces debemos pedirla
        if(location == null){
            // Le decimos que nos traiga la última ubicación del GoogleApiClient
            location = LocationServices.FusedLocationApi.getLastLocation(gac);
            // Tenemos que darle un formato de fecha a tiempo_actualizacion, la cual es la fecha actual del dispositivo
            tiempo_actualizacion = DateFormat.getTimeInstance().format(new Date());
            // Actualizamos la interfaz de usuario
            updateUi();

            /* Si el usuario hace clic en la ubicación antes de que se conecte el GoogleApiClient
            entonces debemos poner la ubicación en verdadero*/
            if(ubicacion){
                startLocationUpdates();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Si se suspende la conexión tratamos de reestablecerla
        Log.i(TAG, "Conexión suspendida");
        gac.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Conexión caída");
    }

    @Override
    public void onLocationChanged(Location location) {
        location = location;
        tiempo_actualizacion = DateFormat.getTimeInstance().format(new Date());
        updateUi();
        Toast.makeText(this, "Localización actualizada", Toast.LENGTH_SHORT).show();
    }

    // Método para guardar la actividad en el Bundle
    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putBoolean(CLAVE_ACTUALIZACION, ubicacion);
        savedInstanceState.putParcelable(CLAVE_UBICACION, location);
        savedInstanceState.putString(ULTIMA_ACTUALIZACION, tiempo_actualizacion);
        super.onSaveInstanceState(savedInstanceState);
    }
}
