export async function  getCategoria() {
   let url='/ciudad/listar1';
    fetch(url, {
        method: 'get',
        headers: {
             'Content-Type': 'application/json'
        }
    })
            .then(response=>{
                console.log(response);
               return response.json();

    })
            .then( data=>{
                console.log(data);
            });
    
}

function promesa() {
    return new Promise((resolve, reject) => {
        setTimeout(() => {
            resolve('Hola');
        }, 1000);
    });
}
export function despedida(){
    console.log('Adios');
}
